package model

import (
	"encoding/json"
	"io"
)

type Request struct {
	Package  string
	Nonce    string
	DeviceID string
}

type TestPayload struct {
	Payload string `json:"payload"`
}

func (reg *TestPayload) Decode(r io.Reader) error {
	return json.NewDecoder(r).Decode(&reg)
}

type ServiceRequest struct {
	Package  string `json:"package"`
	Username string `json:"username"`
	DeviceId string `json:"deviceid"`
	Nonce    string `json:"nonce"`
	NonceEnc string `json:"nonceEnc"`
	Hash     string `json:"hash"`
}

func (req *ServiceRequest) Decode(r io.Reader) error {
	return json.NewDecoder(r).Decode(&req)
}

type ClientAuth struct {
	Auth int `json:auth`
}

func (req *ClientAuth) Decode(r io.Reader) error {
	return json.NewDecoder(r).Decode(&req)
}

type AddPackage struct {
	Package          string `json:"package"`
	UserName         string `json:"email"`
	ProviderUserName string `json:"username"`
}

func (reg *AddPackage) Decode(r io.Reader) error {
	return json.NewDecoder(r).Decode(&reg)
}

type Registration struct {
	UserName string `json:"email"`
	Password string `json:"password"`
	DeviceId string `json:"deviceId"`
	GCMId    string `json""gcmid"`
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
	Provider  string `json:"provider"`
	Key       string `json:"key"`
	Package   string `json:"package"`
	Callback  string `json:"callback"`
	TwoFactor string `json:"twofactor"`
	N         string `json:"N"`
	E         int    `json:"E"`
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
	N string `json:"N"`
	E int    `json:"E"`
}

func (pk *PublicKey) Marshal() ([]byte, error) {
	return json.Marshal(pk)
}

type Data struct {
	MessageId string `json:"messageID"`
	Package   string `json:"package"`
	DeviceId  string `json:"deviceid"`
	Nonce     string `json:"nonce"`
	NonceEnc  string `json:"nonceEnc"`
	Hash      string `json:"hash"`
}

type GcmMessage struct {
	RegIds []string `json:"registration_ids"`
	Data   Data     `json:"data"`
}

func (msg *GcmMessage) Marshal() ([]byte, error) {
	return json.Marshal(msg)
}
