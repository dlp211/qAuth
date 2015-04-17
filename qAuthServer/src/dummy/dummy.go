package main

import (
	"dummy/authenticate"
	"dummy/controller"
	"dummy/db"
	"dummy/model"
	"fmt"
	"logger"
	"net/http"
)

func registerWithQAuth() {
	url := "http://107.170.156.222:8080/provider"
	reg := model.RegisterProvider{
		"DummyServerService",
		"ABCDEFG",
		"qauth.djd.dummyclient",
		"http://107.170.156.222:8081/qauth/callback",
		"http://107.170.156.222:8081/login/twofactor",
		authenticate.PubKey.N.String(),
		authenticate.PubKey.E,
	}

	js, err := reg.Marshal()
	if err != nil {
		panic(err)
	}
	logger.DEBUG(string(js))
	controller.WebRequest("POST", url, js)
}

func init() {
	authenticate.PrivKey = authenticate.LoadPrivKey("DRSAKEY")
	authenticate.PubKey = &authenticate.PrivKey.PublicKey
	authenticate.LoadPubKey()

	controller.DB = db.Init()
	controller.BuildControllerSet()

	pw1, salt1 := authenticate.NewPWHash("password")
	pw2, salt2 := authenticate.NewPWHash("password")
	controller.DB.CreateUser("dlp", pw1, salt1, 10324.89, true)
	controller.DB.CreateUser("dgk", pw2, salt2, 232.54, false)

	registerWithQAuth()

	for endpoint, function := range controller.Controllers {
		http.HandleFunc(endpoint, function)
	}
}

func main() {

	fmt.Println("============================")
	fmt.Println("|| Starting Dummy Server ||")
	fmt.Println("============================")

	fmt.Println(authenticate.PubKey)

	err := http.ListenAndServe(":8081", nil)
	if err != nil {
		panic(err)
	}
}
