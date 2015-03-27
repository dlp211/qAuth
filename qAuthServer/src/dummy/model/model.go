package model

import (
	"encoding/json"
	"io"
)

type Login struct {
	UserName string `json:"email"`
	Password string `json:"password"`
	DeviceId string `json:"deviceId"`
}

func (reg *Login) Decode(r io.Reader) error {
	return json.NewDecoder(r).Decode(&reg)
}
