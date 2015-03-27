package authenticate

import (
	"bufio"
	"crypto/rand"
	"crypto/rsa"
	"crypto/sha1"
	"crypto/x509"
	"encoding/hex"
	"encoding/pem"
	"fmt"
	"logger"
	"os"
)

var AdminKey string
var PrivKey *rsa.PrivateKey
var PubKey *rsa.PublicKey

func AdminAuth(key string) bool {
	return AdminKey == key
}

func Decrypt(payload string) string {
	bytes, _ := hex.DecodeString(payload)
	msg, err := rsa.DecryptOAEP(sha1.New(), nil, PrivKey, bytes, nil)
	if err != nil {
		logger.WARN("Decryption failed")
		panic(err)
	}
	return string(msg)
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
	PubKey = &privKey.PublicKey
	return privKey
}
