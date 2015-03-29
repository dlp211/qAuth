package authenticate

import (
	"bufio"
	"crypto"
	"crypto/rand"
	"crypto/rsa"
	"crypto/sha1"
	"crypto/x509"
	"encoding/hex"
	"encoding/pem"
	"fmt"
	"logger"
	"math/big"
	"os"
	"qauth/db"
	"qauth/model"
)

var AdminKey string
var PROJID string
var GCM string
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

func LoadKey(env string) string {
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

func Hash(un, did, non string) []byte {
	str := un + "" + did + "" + non
	sh := sha1.New()
	hash := sh.Sum([]byte(str))
	return hash
}

func ValidateRequest(req *model.ServiceRequest, DB *db.Tables) (*db.Provider, bool) {
	sha1hash := sha1.New()
	bytes, _ := hex.DecodeString(req.NonceEnc)
	signature, _ := hex.DecodeString(req.Hash)
	if prov, ok := DB.Providers[req.Package]; ok {
		var provPubKey rsa.PublicKey
		provPubKey.N = big.NewInt(0)
		_, _ = provPubKey.N.SetString(prov.Pk.N, 10)
		provPubKey.E = prov.Pk.E
		nonce, err := rsa.DecryptOAEP(sha1hash, rand.Reader, PrivKey, bytes, nil)
		if err != nil {
			panic(err)
		}
		if string(nonce) != req.Nonce {
			logger.WARN("something is wrong")
			logger.WARN("expected:" + req.Nonce)
			logger.WARN("got: " + string(nonce))
		}
		hash := Hash(req.Username, req.DeviceId, req.NonceEnc)
		err = rsa.VerifyPKCS1v15(&provPubKey, crypto.SHA1, hash, []byte(signature))
		if err != nil {
			logger.WARN("DIDN'T VERIFY")
			return &prov, false
		}
		return &prov, true
	}
	return &db.Provider{}, false
}
