package authenticate

import (
	"bufio"
	"crypto"
	"crypto/rand"
	"crypto/rsa"
	"crypto/sha1"
	"crypto/x509"
	"dummy/model"
	"encoding/hex"
	"encoding/pem"
	"fmt"
	"logger"
	"math/big"
	mRand "math/rand"
	"net/http"
	"os"
	"strconv"
	"time"
)

var PrivKey *rsa.PrivateKey
var PubKey *rsa.PublicKey
var PublicServerKey rsa.PublicKey
var maxInt *big.Int = big.NewInt(9223372036854775807)

var letters = []rune("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")

func RandSeq(n int) string {
	b := make([]rune, n)
	mRand.Seed(time.Now().UTC().UnixNano())
	for i := range b {
		b[i] = letters[mRand.Intn(len(letters))]
	}
	return string(b)
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

func EncryptNonce(nonce int64) string {
	non := strconv.FormatInt(nonce, 10)
	msg := []byte(non)
	sha1hash := sha1.New()

	encryptedmsg, err := rsa.EncryptOAEP(sha1hash, rand.Reader, &PublicServerKey, msg, nil)
	if err != nil {
		panic(err)
	}
	return hex.EncodeToString(encryptedmsg)

}

func Hash(un, did, non string) []byte {
	str := un + "" + did + "" + non
	sh := crypto.SHA1.New()
	sh.Write([]byte(str))
	hash := sh.Sum(nil)
	return hash
}

func HashAndSign(un, did, non string) string {
	hash := Hash(un, did, non)
	logger.DEBUG(string(hash))
	bytes, err := rsa.SignPKCS1v15(rand.Reader, PrivKey, crypto.SHA1, hash)
	if err != nil {
		panic(err)
	}
	return hex.EncodeToString(bytes)
}

func GenNonce() (*big.Int, error) {
	return rand.Int(rand.Reader, maxInt)
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

func DecryptNonce(nonceEnc string) string {
	sha1hash := sha1.New()
	bytes, _ := hex.DecodeString(nonceEnc)
	nonce, err := rsa.DecryptOAEP(sha1hash, rand.Reader, PrivKey, bytes, nil)
	if err != nil {
		panic(err)
	}
	return string(nonce)
}

func ValidateCallbackResult(result *model.CallbackResult) (int64, bool) {
	signature, _ := hex.DecodeString(result.Hash)
	hash := Hash(result.Token1, result.Token2, result.NonceEnc)
	err := rsa.VerifyPKCS1v15(&PublicServerKey, crypto.SHA1, hash, []byte(signature))
	if err != nil {
		logger.WARN("DIDN'T VERIFY")
		return 0, false
	}
	val, _ := strconv.ParseInt(DecryptNonce(result.NonceEnc), 10, 64)
	return val, true
}
