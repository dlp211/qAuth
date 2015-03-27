package main

import (
	"dummy/authenticate"
	"dummy/controller"
	"dummy/db"
	"fmt"
	"net/http"
)

func init() {
	controller.DB = db.Init()
	controller.BuildControllerSet()

	for endpoint, function := range controller.Controllers {
		http.HandleFunc(endpoint, function)
	}
}

func main() {

	fmt.Println("============================")
	fmt.Println("|| Starting Dummy Server ||")
	fmt.Println("============================")

	pw1, salt1 := authenticate.NewPWHash("password")
	pw2, salt2 := authenticate.NewPWHash("password")
	controller.DB.CreateUser("dlp", pw1, salt1, true)
	controller.DB.CreateUser("djk", pw2, salt2, false)

	err := http.ListenAndServe(":8081", nil)
	if err != nil {
		panic(err)
	}
}
