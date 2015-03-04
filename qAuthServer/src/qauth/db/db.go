package db

import (
	"fmt"
	"qauth/model"
)

type user struct {
	password    string
	salt        string
	deviceId    []string
	bluetoothId string
	pk          model.PublicKey
}

func (u *user) String() string {
	return fmt.Sprintf("%s", u.deviceId)
}

type provider struct {
	key         string
	packageName string
	callback    string
}

type Tables struct {
	Users     map[string]user
	Providers map[string]provider
}

func Init() *Tables {
	var DB Tables
	DB.Users = map[string]user{}
	DB.Providers = map[string]provider{}
	return &DB
}

func (DB *Tables) CreateUser(reg *model.Registration, hashedPW string, salt string) {
	DB.Users[reg.UserName] = user{
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
