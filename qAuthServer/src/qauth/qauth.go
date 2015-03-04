package main

import (
	//"crypto/rsa"
	//"crypto/x509"
	//"encoding/pem"
	"fmt"
	"logger"
	"net/http"
	//"os"
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

	fmt.Println("LOGGER TESTS\n==================")
	logger.INFO("INFO")
	logger.DEBUG("DEBUG")
	logger.WARN("WARN")
	logger.PANIC("PANIC")

	pubKey := authenticate.PrivKey.Public()

	fmt.Println("PUBLIC KEY")
	fmt.Println(pubKey)

	/*
		pemfile, _ := os.Create("pub.key")
		pub, _ := x509.MarshalPKIXPublicKey(pubKey)
		var pemkey = &pem.Block{Type: "RSA PUBLIC KEY", Bytes: pub}
		pem.Encode(pemfile, pemkey)
		fmt.Println(pub) */

	fmt.Println(authenticate.AdminKey)
	http.ListenAndServe(":8080", nil)
}
