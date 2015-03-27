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
)

var Controllers = map[string]func(http.ResponseWriter, *http.Request){}
var DB *db.Tables

func launchTwoFactor(deviceid, username string) {
	logger.DEBUG("here")
	url := "http://107.170.156.222:8080/authenticate"

	var jsonStr = []byte(`{ "deviceid":"` + deviceid + `", "username":` + username + `", "nonce":123  }`)
	logger.DEBUG(string(jsonStr))
	req, err := http.NewRequest("POST", url, bytes.NewBuffer(jsonStr))
	req.Header.Set("Content-Type", "application/json")

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		panic(err)
	}
	defer resp.Body.Close()

	logger.INFO(fmt.Sprintf("response Status:", resp.Status))
	logger.INFO(fmt.Sprintf("response Headers:", resp.Header))

	body, _ := ioutil.ReadAll(resp.Body)
	logger.INFO(fmt.Sprintf("*ANDROID POST* response: ", string(body)))
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
		logger.DEBUG("OK")
		if authenticate.Password(login.Password, user.Salt, user.Password) {
			if user.TwoFactor {
				logger.DEBUG("TF")
				launchTwoFactor(login.UserName, login.DeviceId)
				w.WriteHeader(http.StatusUnauthorized)
			} else {
				logger.DEBUG("SF")
				w.WriteHeader(http.StatusAccepted)
			}
		} else {
			logger.DEBUG("WTF")
		}
	} else {
		logger.WARN("Unregistered user " + login.UserName + " attempted to login")
		w.WriteHeader(http.StatusConflict)
	}
}

func BuildControllerSet() {
	Controllers["/login"] = Login
}
