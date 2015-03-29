package model

import (
	"encoding/json"
	"io"
	"time"
)

type Request struct {
	UserName string
	Gpa      float32
	Nonce    int64
	DeviceId string
	Token1   string
	Token2   string
}

type Session struct {
	Username   string
	Expiration time.Time
}

type AuthRequest struct {
	Package  string `json:"package"`
	UserName string `json:"username"`
	DeviceId string `json:"deviceid"`
	Nonce    string `json:"nonce"`
	NonceEnc string `json:"nonceEnc"`
	Hash     string `json:"hash"`
}

func (auth *AuthRequest) Marshal() ([]byte, error) {
	return json.Marshal(auth)
}

type Login struct {
	UserName string `json:"username"`
	Password string `json:"password"`
	DeviceId string `json:"deviceId"`
}

func (reg *Login) Decode(r io.Reader) error {
	return json.NewDecoder(r).Decode(&reg)
}

type CallbackResult struct {
	Token1   string `json:"token1"`
	Token2   string `json:"token2"`
	Nonce    string `json:"nonce"`
	NonceEnc string `json:nonceEnc`
	Hash     string `json:"hash"`
}

func (res *CallbackResult) Decode(r io.Reader) error {
	return json.NewDecoder(r).Decode(&res)
}

type Token struct {
	Token string `json:"token"`
}

type TwofactorResult struct {
	Token string `json:"token"`
	Data  Data   `json:"data"`
}

func (reg *Token) Decode(r io.Reader) error {
	return json.NewDecoder(r).Decode(&reg)
}

func (reg *TwofactorResult) Marshal() ([]byte, error) {
	return json.Marshal(reg)
}

type PublicKey struct {
	N string `json:"N"`
	E int    `json:"E"`
}

func (pk *PublicKey) Decode(r io.Reader) error {
	return json.NewDecoder(r).Decode(pk)
}

type RegisterProvider struct {
	Provider  string `json:"provider"`
	Key       string `json:"key"`
	Package   string `json:"package"`
	Callback  string `json:"callback"`
	TwoFactor string `json:"twofactor"`
	N         string `json:"N"`
	E         int    `json:"E"`
}

func (reg *RegisterProvider) Marshal() ([]byte, error) {
	return json.Marshal(reg)
}

type Data struct {
	Gpa       float32 `json:"gpa"`
	SessionId string  `json:"sessionid"`
}

func (d *Data) Marshal() ([]byte, error) {
	return json.Marshal(d)
}
