package main

import (
	"crypto/rsa"
	"fmt"
	"logger"
	"net/http"
	"qauth/authenticate"
	"qauth/controller"
	"qauth/db"
)

var adminKey string
var privKey *rsa.PrivateKey
var DB *db.Tables

func init() {
	adminKey = "AAAA" /* This will be changed to pull from an ENV variable */
	privKey = authenticate.LoadPrivKey("RSAKey")

	DB = db.Init()
	controller.BuildControllerSet(DB)

	for endpoint, function := range controller.Controllers {
		http.HandleFunc(endpoint, function)
	}
}

func main() {

	fmt.Println("============================")
	fmt.Println("|| Starting Q-Auth Server ||")
	fmt.Println("============================")

	fmt.Println("LOGGER TESTS\n==================")
	logger.INFO("INFO")
	logger.DEBUG("DEBUG")
	logger.WARN("WARN")
	logger.PANIC("PANIC")

	pubKey := privKey.Public()

	fmt.Println("PUBLIC KEY")
	fmt.Println(pubKey)

	http.ListenAndServe(":8080", nil)
}
