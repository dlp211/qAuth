package main

import (
	"fmt"
	"net/http"
	"qauth/authenticate"
	"qauth/controller"
	"qauth/db"
)

func init() {
	authenticate.AdminKey = authenticate.LoadAdminKey("ADMINKEY")
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

	http.ListenAndServe(":8080", nil)
}
