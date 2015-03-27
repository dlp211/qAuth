package authenticate

import (
	"bufio"
	"crypto/rand"
	"crypto/rsa"
	"crypto/sha1"
	"crypto/x509"
	"dummy/model"
	"encoding/pem"
	"fmt"
	"logger"
	"math/big"
	"net/http"
	"os"
)

var PrivKey *rsa.PrivateKey
var PubKey *rsa.PublicKey
var PublicServerKey rsa.PublicKey

func Password(password, salt, hash string) bool {
	hasher := sha1.New()
	hasher.Write([]byte(password + salt))
	return fmt.Sprintf("% x", hasher.Sum(nil)) == hash
}

func NewPWHash(password string) (string, string) {
	b := make([]byte, 64)
	_, err := rand.Read(b)
	if err != nil {
		panic(err)
	}
	salt := string(b)
	hasher := sha1.New()
	hasher.Write([]byte(password + salt))
	return fmt.Sprintf("% x", hasher.Sum(nil)), salt
}

func LoadPubKey() {
	logger.DEBUG("here")
	url := "http://107.170.156.222:8080/public/key"

	req, err := http.NewRequest("GET", url, nil)

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		panic(err)
	}
	defer resp.Body.Close()

	var pk model.PublicKey
	err = pk.Decode(resp.Body)
	if err != nil {
		logger.WARN("Error wans't nil")
		panic(err)
	}
	logger.INFO(pk.N)
	PublicServerKey.N = big.NewInt(0)
	_, _ = PublicServerKey.N.SetString(pk.N, 10)
	PublicServerKey.E = pk.E
}

func LoadPrivKey(env string) *rsa.PrivateKey {
	file := os.Getenv(env)

	pemfile, err := os.Open(file)
	if err != nil {
		panic(err)
	}

	pemfileinfo, _ := pemfile.Stat()
	defer pemfile.Close()
	var size int64 = pemfileinfo.Size()
	pembytes := make([]byte, size)

	buffer := bufio.NewReader(pemfile)
	_, err = buffer.Read(pembytes)

	data, _ := pem.Decode([]byte(pembytes))

	privKey, err := x509.ParsePKCS1PrivateKey(data.Bytes)
	if err != nil {
		logger.PANIC("PRIVATE KEY FAILED TO LOAD")
		panic(err)
	}
	return privKey
}
