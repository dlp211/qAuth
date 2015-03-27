package controller

import (
	"dummy/authenticate"
	"dummy/db"
	"dummy/model"
	"logger"
	"net/http"
)

var Controllers = map[string]func(http.ResponseWriter, *http.Request){}
var DB *db.Tables

func launchTwoFactor() {
}

func Login(w http.ResponseWriter, r *http.Request) {
	logger.INFO("/login")
	var login model.Login
	err := login.Decode(r.Body)
	if err != nil {
		panic(err)
	}

	if user, ok := DB.Users[login.UserName]; ok {
		if authenticate.Password(login.Password, user.Password, user.Salt) {
			if user.TwoFactor {
				launchTwoFactor()
			} else {
				w.WriteHeader(http.StatusAccepted)
			}
		}
	} else {
		logger.WARN("Unregistered user " + login.UserName + " attempted to login")
		w.WriteHeader(http.StatusConflict)
	}
}

func BuildControllerSet() {
	Controllers["/login"] = Login
}
