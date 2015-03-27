package authenticate

import (
	"crypto/rand"
	"crypto/sha1"
	"fmt"
)

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
