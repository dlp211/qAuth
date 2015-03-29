package main

import (
	"bytes"
	"dummy/authenticate"
	"dummy/controller"
	"dummy/db"
	"dummy/model"
	"fmt"
	"io/ioutil"
	"logger"
	"net/http"
)

func registerWithQAuth() {
	logger.DEBUG("here")
	url := "http://107.170.156.222:8080/provider"

	reg := model.RegisterProvider{
		"DummyServerService",
		"ABCDEFG",
		"qauth.djd.dummyclient",
		"http://107.170.156.222:8081/callback",
		"http://107.170.156.222:8081/login/twofactor",
		authenticate.PubKey.N.String(),
		authenticate.PubKey.E,
	}
	js, err := reg.Marshal()
	if err != nil {
		panic(err)
	}
	logger.DEBUG(string(js))
	req, err := http.NewRequest("POST", url, bytes.NewBuffer(js))
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

func init() {
	authenticate.PrivKey = authenticate.LoadPrivKey("DRSAKEY")
	authenticate.PubKey = &authenticate.PrivKey.PublicKey
	authenticate.LoadPubKey()

	controller.DB = db.Init()
	controller.BuildControllerSet()

	pw1, salt1 := authenticate.NewPWHash("password")
	pw2, salt2 := authenticate.NewPWHash("password")
	controller.DB.CreateUser("dlp", pw1, salt1, true)
	controller.DB.CreateUser("djk", pw2, salt2, false)

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
