package controller

import (
	"bytes"
	"dummy/authenticate"
	"dummy/db"
	"dummy/model"
	"fmt"
	"io/ioutil"
	"logger"
	"net/http"
	"time"
)

var Controllers = map[string]func(http.ResponseWriter, *http.Request){}

//DB of users, no registration, current 2 users, 1FA and 2FA
var DB *db.Tables
var Package = "qauth.djd.dummyclient"

// Holds a single request for 2FA (current limitation)
var request model.Request

// Holds all the sessions, clients must pass sessionID string in order to continue authentication
var Session = map[string]model.Session{}

func WebRequest(protocol, url string, js []byte) {
	req, err := http.NewRequest(protocol, url, bytes.NewBuffer(js))
	req.Header.Set("Content-Type", "application/json")

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		panic(err)
	}
	defer resp.Body.Close()

	logger.INFO(fmt.Sprintf("response Status: ", resp.Status))
	logger.INFO(fmt.Sprintf("response Headers: ", resp.Header))
	body, _ := ioutil.ReadAll(resp.Body)
	logger.INFO(fmt.Sprintf("response Body: ", string(body)))
}

/*
 * Helper function to get 2FA started for a given client
 */
func launchTwoFactor(username, deviceid string) {
	url := "http://107.170.156.222:8080/authenticate"

	nonce := authenticate.EncryptNonce(request.Nonce)
	authReq := model.AuthRequest{
		Package,
		username,
		deviceid,
		nonce,
		authenticate.HashAndSign(username, deviceid, nonce),
	}

	js, err := authReq.Marshal()
	if err != nil {
		panic(err)
	}

	logger.DEBUG(string(js))
	WebRequest("POST", url, js)
}

/*
 * Main Entry Point for a User
 * Scenarios:
 *  1: User Authenticates
 *  2: User Needs 2FA
 *  3: User is not in DB
 *  4: User entered wrong Password
 */
func Login(w http.ResponseWriter, r *http.Request) {
	logger.INFO("/login")
	var login model.Login
	err := login.Decode(r.Body)
	if err != nil {
		panic(err)
	}

	if user, ok := DB.Users[login.UserName]; ok {
		if authenticate.Password(login.Password, user.Salt, user.Password) {
			if user.TwoFactor {
				nonce, err := authenticate.GenNonce()
				if err != nil {
					panic(err)
				}
				request = model.Request{login.UserName, user.Balance, nonce.Int64(), login.DeviceId, login.GcmId, "", ""}
				launchTwoFactor(login.UserName, login.DeviceId)
				w.WriteHeader(http.StatusAccepted)
			} else {
				session := authenticate.RandSeq(10)
				d := model.Data{user.Balance, session}
				js, err := d.Marshal()
				if err != nil {
					panic(err)
				}
				Session[session] = model.Session{login.UserName, time.Now().Add(time.Minute * 30), login.GcmId}
				w.WriteHeader(http.StatusOK)
				w.Header().Set("Content-Type", "application/json")
				w.Write(js)
			}
		} else {
			logger.WARN("Wrong Password")
			w.WriteHeader(http.StatusUnauthorized)
		}
	} else {
		logger.WARN("Unregistered user " + login.UserName + " attempted to login")
		w.WriteHeader(http.StatusUnauthorized)
	}
}

/*
 * This is the endpoint that qAuth hits with its tokens
 * Check that nonce == nonce + 3
 */
func Callback(w http.ResponseWriter, r *http.Request) {
	logger.INFO("/callback")
	var res model.CallbackResult
	err := res.Decode(r.Body)
	if err != nil {
		panic(err)
	}
	if nonce, ok := authenticate.ValidateCallbackResult(&res); ok {
		if nonce-3 == request.Nonce {
			request.Token1 = authenticate.Decrypt(res.Token1)
			request.Token2 = authenticate.Decrypt(res.Token2)
		} else {
			logger.WARN(fmt.Sprintf("%v", nonce-3))
			logger.WARN(fmt.Sprintf("%v", request.Nonce))
			logger.WARN("bad nonce")
		}
	}
}

/*
 * This is the callback for the qAuth client
 * If the returned token matches, start a session and return our token
 */
func TwoFactor(w http.ResponseWriter, r *http.Request) {
	logger.INFO("/login/twofactor")
	var token model.Token
	err := token.Decode(r.Body)
	if err != nil {
		panic(err)
	}
	logger.DEBUG(token.Token)
	logger.DEBUG(request.Token1)
	if token.Token == request.Token1 {
		session := authenticate.RandSeq(10)
		Session[session] = model.Session{request.UserName, time.Now().Add(time.Minute * 30), request.GcmId}

		twRes := model.TwofactorResult{request.Token2, model.Data{request.Balance, session}}
		js, err := twRes.Marshal()
		if err != nil {
			panic(err)
		}
		logger.INFO(fmt.Sprintf("%v", twRes))
		w.Header().Set("Content-Type", "application/json")
		w.Write(js)
		w.WriteHeader(http.StatusAccepted)
	} else {
		logger.DEBUG(":(")
		w.WriteHeader(http.StatusUnauthorized)
	}
}

/*
 * Let's a user update their account balance
 */
func UpdateAccount(w http.ResponseWriter, r *http.Request) {
	logger.INFO("/account/update")
	var update model.AcctUpdate
	err := update.Decode(r.Body)
	if err != nil {
		panic(err)
	}
	if session, ok := Session[update.SessionId]; ok {
		if !session.Expiration.After(time.Now()) {
			delete(Session, update.SessionId)
			w.WriteHeader(http.StatusGone)
			return
		}
		if user, ok := DB.Users[session.Username]; ok {
			if user.Balance+update.Amount > 0.0 {
				logger.DEBUG(fmt.Sprintf("Balance: %v", user.Balance))
				user.Balance += update.Amount
				logger.DEBUG(fmt.Sprintf("Balance: %v", user.Balance))
				DB.Users[session.Username] = user
				session.Expiration = time.Now().Add(time.Minute * 30)
				data := model.Data{user.Balance, update.SessionId}
				js, err := data.Marshal()
				if err != nil {
					panic(err)
				}
				w.WriteHeader(http.StatusAccepted)
				w.Header().Set("Content-Type", "application/json")
				w.Write(js)
			} else {
				w.WriteHeader(http.StatusNotAcceptable)
			}
		} else {
			w.WriteHeader(http.StatusNotFound)
		}
	} else {
		w.WriteHeader(http.StatusUnauthorized)
	}
}

func logout(gcmid string) {
	url := "https://android.googleapis.com/gcm/send"

	gcm := model.GcmMessage{[]string{gcmid}, model.GcmData{"0"}}

	js, _ := gcm.Marshal()

	req, err := http.NewRequest("POST", url, bytes.NewBuffer(js))
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("project_id", "542773875626")
	req.Header.Set("Authorization", "key=AIzaSyDFz1j1UvitL_ee2wSl2dCzKjeUDcR3N_k")

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		panic(err)
	}
	defer resp.Body.Close()

	body, _ := ioutil.ReadAll(resp.Body)
	logger.DEBUG("response: " + string(body))
}

func LoginSession(w http.ResponseWriter, r *http.Request) {
	logger.INFO("/login/session")
	var login model.LoginSession
	err := login.Decode(r.Body)
	if err != nil {
		panic(err)
	}
	if session, ok := Session[login.SessionId]; ok {
		if !session.Expiration.After(time.Now()) {
			delete(Session, login.SessionId)
			w.WriteHeader(http.StatusGone)
			return
		}
		if session.GcmId == login.OldId {
			session.GcmId = login.NewId
			logout(login.OldId)
			session.Expiration = time.Now().Add(time.Minute * 30)
			if user, ok := DB.Users[session.Username]; ok {
				data := model.Data{user.Balance, login.SessionId}
				js, err := data.Marshal()
				if err != nil {
					panic(err)
				}
				w.WriteHeader(http.StatusAccepted)
				w.Header().Set("Content-Type", "application/json")
				w.Write(js)
			}
		} else {
			w.WriteHeader(http.StatusUnauthorized)
		}
	} else {
		w.WriteHeader(http.StatusNotFound)
	}
}

func BuildControllerSet() {
	Controllers["/login"] = Login
	Controllers["/qauth/callback"] = Callback
	Controllers["/login/twofactor"] = TwoFactor
	Controllers["/account/update"] = UpdateAccount
	Controllers["/login/session"] = LoginSession
}
