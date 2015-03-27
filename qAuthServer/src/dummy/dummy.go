package main

import (
	"dummy/authenticate"
	"dummy/controller"
	"dummy/db"
	"fmt"
	"net/http"
)

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
