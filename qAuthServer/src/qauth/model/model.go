package model

import (
	"encoding/json"
	"io"
)

type Request struct {
	Package  string
	UserName string
	Nonce    string
	DeviceID string
}

type TestPayload struct {
	Payload string `json:"payload"`
}

func (reg *TestPayload) Decode(r io.Reader) error {
	return json.NewDecoder(r).Decode(&reg)
}

// Incoming pkg from a service
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

// This needs to be updated
type ClientAuth struct {
	Auth     int    `json:auth`
	Nonce    string `json:"nonce"`
	NonceEnc string `json:"nonceEnc"`
	Hash     string `json:"hash"`
}

func (req *ClientAuth) Decode(r io.Reader) error {
	return json.NewDecoder(r).Decode(&req)
}

// Request to add a user to a provider
type AddPackage struct {
	Package          string `json:"package"`
	UserName         string `json:"email"`
	ProviderUserName string `json:"username"`
}

func (reg *AddPackage) Decode(r io.Reader) error {
	return json.NewDecoder(r).Decode(&reg)
}

// User registration
type Registration struct {
	UserName string `json:"email"`
	Password string `json:"password"`
	DeviceId string `json:"deviceId"`
	GCMId    string `json""gcmid"`
}

func (reg *Registration) Decode(r io.Reader) error {
	return json.NewDecoder(r).Decode(&reg)
}

// Register BT device
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

// Provider Registration
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

//Admin Authorization packet
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

// Data for GCM message, TODO: Add another data type to clean this up
type Data struct {
	MessageId   string      `json:"messageID"`
	BluetoothId string      `json:"bluetoothId"`
	Package     string      `json:"package"`
	DeviceId    string      `json:"deviceid"`
	Nonce       string      `json:"nonce"`
	NonceEnc    string      `json:"nonceEnc"`
	Hash        string      `json:"hash"`
	Callback    string      `json:"callback"`
	TokenResult TokenResult `json:"tokenResult"`
}

type GcmMessage struct {
	RegIds []string `json:"registration_ids"`
	Data   Data     `json:"data"`
}

func (msg *GcmMessage) Marshal() ([]byte, error) {
	return json.Marshal(msg)
}

type TokenResult struct {
	Token1   string `json:"token1"`
	Token2   string `json:"token2"`
	Nonce    string `json:"nonce"`
	NonceEnc string `json:"nonceEnc"`
	Hash     string `json:"hash"`
}

func (tk *TokenResult) Marshal() ([]byte, error) {
	return json.Marshal(tk)
}
