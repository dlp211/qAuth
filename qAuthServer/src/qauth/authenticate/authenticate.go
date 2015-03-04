package authenticate

import (
	"bufio"
	"crypto/rand"
	"crypto/rsa"
	"crypto/sha1"
	"crypto/x509"
	"encoding/pem"
	"fmt"
	"logger"
	"os"
)

var AdminKey string
var PrivKey *rsa.PrivateKey

func AdminAuth(key string) bool {
	return AdminKey == key
}

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

func LoadAdminKey(env string) string {
	return os.Getenv(env)
}

func LoadPrivKey(env string) *rsa.PrivateKey {
	file := os.Getenv(env)

	pemfile, err := os.Open(file)
	if err != nil {
		panic(err)
	}

	// need to convert pemfile to []byte for decoding
	pemfileinfo, _ := pemfile.Stat()
	var size int64 = pemfileinfo.Size()
	pembytes := make([]byte, size)

	// read pemfile content into pembytes
	buffer := bufio.NewReader(pemfile)
	_, err = buffer.Read(pembytes)

	// proper decoding now
	data, _ := pem.Decode([]byte(pembytes))

	pemfile.Close()

	fmt.Printf("PEM Type : %s\n", data.Type)
	fmt.Printf("PEM Headers : %s\n", data.Headers)
	fmt.Printf("PEM Bytes : %x\n", string(data.Bytes))

	privKey, err := x509.ParsePKCS1PrivateKey(data.Bytes)
	if err != nil {
		logger.PANIC("PRIVATE KEY FAILED TO LOAD")
		panic(err)
	}
	return privKey
}
