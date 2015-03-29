package main

import (
	"fmt"
	"net/http"
	"qauth/authenticate"
	"qauth/controller"
	"qauth/db"
)

func init() {
	authenticate.AdminKey = authenticate.LoadKey("ADMINKEY")
	authenticate.PROJID = authenticate.LoadKey("PROJID")
	authenticate.GCM = authenticate.LoadKey("GCMKEY")
	authenticate.PrivKey = authenticate.LoadPrivKey("RSAKEY")

	controller.DB = db.Init()
	controller.BuildControllerSet()

	for endpoint, function := range controller.Controllers {
		http.HandleFunc(endpoint, function)
	}
}

func main() {

	fmt.Println("============================")
	fmt.Println("|| Starting Q-Auth Server ||")
	fmt.Println("============================")

	pubKey := authenticate.PrivKey.Public()

	fmt.Println("PUBLIC KEY\n==================================")
	fmt.Println(pubKey)

	err := http.ListenAndServe(":8080", nil)
	if err != nil {
		panic(err)
	}
}
