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
	"strconv"
	"time"
)

var Controllers = map[string]func(http.ResponseWriter, *http.Request){}
var DB *db.Tables
var Package = "qauth.djd.dummyclient"

var request model.Request
var Session map[string]model.Session

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

func launchTwoFactor(username, deviceid string) {
	url := "http://107.170.156.222:8080/authenticate"

	nonce := authenticate.EncryptNonce(request.Nonce)
	authReq := model.AuthRequest{
		Package,
		username,
		deviceid,
		strconv.FormatInt(request.Nonce, 10),
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

func Login(w http.ResponseWriter, r *http.Request) {
	logger.INFO("/login")
	var login model.Login
	err := login.Decode(r.Body)
	if err != nil {
		panic(err)
	}

	logger.DEBUG(login.UserName + " " + login.Password + " " + login.DeviceId)
	if user, ok := DB.Users[login.UserName]; ok {
		if authenticate.Password(login.Password, user.Salt, user.Password) {
			if user.TwoFactor {
				nonce, err := authenticate.GenNonce()
				if err != nil {
					panic(err)
				}
				request = model.Request{login.UserName, user.GPA, nonce.Int64(), login.DeviceId, "", ""}
				launchTwoFactor(login.UserName, login.DeviceId)
				w.WriteHeader(http.StatusAccepted)
			} else {
				d := model.Data{user.GPA, "ABCDEFG"}
				js, err := d.Marshal()
				if err != nil {
					panic(err)
				}
				Session["ABCDEFG"] = model.Session{login.UserName, time.Now().Add(time.Minute * 30)}
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

func Callback(w http.ResponseWriter, r *http.Request) {
	logger.INFO("/callback")
	var res model.CallbackResult
	err := res.Decode(r.Body)
	if err != nil {
		panic(err)
	}
	if ok := authenticate.ValidateCallbackResult(&res); ok {
		nonce, _ := strconv.ParseInt(res.Nonce, 10, 64)
		if nonce == request.Nonce {
			request.Token1 = res.Token1
			request.Token2 = res.Token2
		}
	}
}

func TwoFactor(w http.ResponseWriter, r *http.Request) {
	logger.INFO("/login/twofactor")
	var token model.Token
	err := token.Decode(r.Body)
	if err != nil {
		panic(err)
	}
	if token.Token == request.Token1 {
		Session["GFEDCBA"] = model.Session{request.UserName, time.Now().Add(time.Minute * 30)}

		twRes := model.TwofactorResult{request.Token2, model.Data{request.Gpa, "GFEDCBA"}}
		js, err := twRes.Marshal()
		if err != nil {
			panic(err)
		}
		w.WriteHeader(http.StatusAccepted)
		w.Header().Set("Content-Type", "application/json")
		w.Write(js)
	} else {
		logger.DEBUG(":(")
		w.WriteHeader(http.StatusUnauthorized)
	}
}

func BuildControllerSet() {
	Controllers["/login"] = Login
	Controllers["/qauth/callback"] = Callback
	Controllers["/login/twofactor"] = TwoFactor
}
