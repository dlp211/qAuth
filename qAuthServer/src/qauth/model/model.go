package model

import (
	"encoding/json"
	"io"
)

type TestPayload struct {
	Payload string `json:payload`
}

func (reg *TestPayload) Decode(r io.Reader) error {
	return json.NewDecoder(r).Decode(&reg)
}

type ServiceRequest struct {
	Username string `json:username`
	DeviceId string `json:deviceid`
	Nonce    int    `json:nonce`
}

func (req *ServiceRequest) Decode(r io.Reader) error {
	return json.NewDecoder(r).Decode(&req)
}

type AddPackage struct {
	Package          string `json:package`
	UserName         string `json:email`
	ProviderUserName string `json:username`
}

func (reg *AddPackage) Decode(r io.Reader) error {
	return json.NewDecoder(r).Decode(&reg)
}

type Registration struct {
	UserName string `json:"email"`
	Password string `json:"password"`
	DeviceId string `json:"deviceId"`
}

func (reg *Registration) Decode(r io.Reader) error {
	return json.NewDecoder(r).Decode(&reg)
}

type RegisterBT struct {
	UserName    string `json:"email"`
	Password    string `json:"password"`
	BluetoothId string `json:"bluetoothId"`
	PKN         string `json:"publicKey_n"`
	PKE         int    `json:"publicKey_e"`
}

func (reg *RegisterBT) Decode(r io.Reader) error {
	return json.NewDecoder(r).Decode(&reg)
}

type RegisterProvider struct {
	Provider string `json:"provider"`
	Key      string `json:"key"`
	Package  string `json:"package"`
	Callback string `json:"callback"`
}

func (reg *RegisterProvider) Decode(r io.Reader) error {
	return json.NewDecoder(r).Decode(&reg)
}

type PackageList struct {
	Packages []string `json:"packages"`
}

type AdminDBAccess struct {
	File string `json:"file"`
	Key  string `json:"key"`
}

func (admin *AdminDBAccess) Decode(r io.Reader) error {
	return json.NewDecoder(r).Decode(admin)
}

type PublicKey struct {
	N string
	E int
}
