package model

import (
	"encoding/json"
	"io"
)

type Login struct {
	UserName string `json:"username"`
	Password string `json:"password"`
	DeviceId string `json:"deviceId"`
}

func (reg *Login) Decode(r io.Reader) error {
	return json.NewDecoder(r).Decode(&reg)
}

type Tokens struct {
	Token1 string `json:"token1"`
	Token2 string `json:"token2"`
}

func (reg *Tokens) Decode(r io.Reader) error {
	return json.NewDecoder(r).Decode(&reg)
}

type Token struct {
	Token string `json:"token"`
}

func (reg *Token) Decode(r io.Reader) error {
	return json.NewDecoder(r).Decode(&reg)
}

func (reg *Token) Marshal() ([]byte, error) {
	return json.Marshal(reg)
}

type PublicKey struct {
	N string `json:"N"`
	E int    `json:"E"`
}

func (pk *PublicKey) Decode(r io.Reader) error {
	return json.NewDecoder(r).Decode(pk)
}
