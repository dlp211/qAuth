package db

import (
	"fmt"
	"qauth/model"
)

type User struct {
	password    string
	salt        string
	deviceId    []string
	bluetoothId string
	pk          model.PublicKey
}

func (u *User) String() string {
	return fmt.Sprintf("%s", u.deviceId)
}

type Provider struct {
	key         string
	packageName string
	callback    string
}

type Tables struct {
	Users     map[string]User
	Providers map[string]Provider
}

func Init() *Tables {
	var DB Tables
	DB.Users = map[string]User{}
	DB.Providers = map[string]Provider{}
	return &DB
}

func (DB *Tables) CreateUser(reg *model.Registration, hashedPW string, salt string) {
	DB.Users[reg.UserName] = User{
		hashedPW,
		salt,
		[]string{reg.DeviceId},
		"",
		model.PublicKey{"", 0},
	}
}

func (table *Tables) save(file string) {
}

func (table *Tables) load(file string) {
}
