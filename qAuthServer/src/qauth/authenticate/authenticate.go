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
	"strconv"
)

var AdminKey string
var PROJID string
var GCM string
var PrivKey *rsa.PrivateKey
var PubKey *rsa.PublicKey

var maxInt *big.Int = big.NewInt(9223372036854775807)

func AdminAuth(key string) bool {
	return AdminKey == key
}

func GenTokens() (*big.Int, *big.Int) {
	tk1, _ := rand.Int(rand.Reader, maxInt)
	tk2, _ := rand.Int(rand.Reader, maxInt)
	return tk1, tk2
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
	sh := crypto.SHA1.New()
	sh.Write([]byte(str))
	hash := sh.Sum(nil)
	return hash
}

func DecryptNonce(nonceEnc string) string {
	sha1hash := sha1.New()
	bytes, _ := hex.DecodeString(nonceEnc)
	nonce, err := rsa.DecryptOAEP(sha1hash, rand.Reader, PrivKey, bytes, nil)
	if err != nil {
		panic(err)
	}
	return string(nonce)
}

func ValidateRequest(req *model.ServiceRequest, DB *db.Tables) (*db.Provider, bool) {
	signature, _ := hex.DecodeString(req.Hash)
	if prov, ok := DB.Providers[req.Package]; ok {
		var provPubKey rsa.PublicKey
		provPubKey.N = big.NewInt(0)
		_, _ = provPubKey.N.SetString(prov.Pk.N, 10)
		provPubKey.E = prov.Pk.E
		hash := Hash(req.Username, req.DeviceId, req.NonceEnc)
		err := rsa.VerifyPKCS1v15(&provPubKey, crypto.SHA1, hash, []byte(signature))
		if err != nil {
			logger.WARN("DIDN'T VERIFY")
			return &prov, false
		}
		return &prov, true
	}
	return &db.Provider{}, false
}

func ValidateClientAuthroization(auth *model.ClientAuth, pk *rsa.PublicKey, nonce string) bool {
	signature, _ := hex.DecodeString(auth.Hash)
	authorized := auth.Auth == 1 && nonce == DecryptNonce(auth.NonceEnc)
	if !authorized {
		logger.WARN("ValidateClient Auth FAILED")
	}

	hash := Hash(strconv.Itoa(auth.Auth), auth.NonceEnc, "")
	logger.INFO(hash)
	err := rsa.VerifyPKCS1v15(pk, crypto.SHA1, hash, []byte(signature))
	if err != nil {
		logger.WARN("DIDN'T VERIFY")
		return false
	}
	return authorized
}

func IncNonce(nonce string, val int64) string {
	non, _ := strconv.ParseInt(nonce, 10, 64)
	non += val
	return strconv.FormatInt(non, 10)
}

func Encrypt(non string, pk *rsa.PublicKey) string {
	msg := []byte(non)
	sha1hash := sha1.New()
	encryptedmsg, err := rsa.EncryptOAEP(sha1hash, rand.Reader, pk, msg, nil)
	if err != nil {
		panic(err)
	}
	return hex.EncodeToString(encryptedmsg)
}

func HashAndSign(one, two, three string) string {
	hash := Hash(one, two, three)
	logger.DEBUG("")
	logger.DEBUG(hex.EncodeToString(hash))
	logger.DEBUG("")
	bytes, err := rsa.SignPKCS1v15(rand.Reader, PrivKey, crypto.SHA1, hash)
	if err != nil {
		panic(err)
	}
	logger.DEBUG("")
	logger.DEBUG(hex.EncodeToString(bytes))
	logger.DEBUG("")
	return hex.EncodeToString(bytes)
}
