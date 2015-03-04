package db

import (
	"fmt"
	"qauth/model"
)

type User struct {
	Password    string
	Salt        string
	DeviceId    []string
	BluetoothId string
	Pk          model.PublicKey
}

func (u *User) String() string {
	return fmt.Sprintf("%s %v", u.DeviceId, u.Pk)
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

func (DB *Tables) UpdateUser(name string, user *User) bool {
	if val, ok := DB.Users[name]; ok {
		val.Password = user.Password
		val.Salt = user.Salt
		val.DeviceId = user.DeviceId
		val.BluetoothId = user.BluetoothId
		val.Pk = user.Pk
		DB.Users[name] = val
		return true
	}
	return false
}

func (table *Tables) save(file string) {
}

func (table *Tables) load(file string) {
}
