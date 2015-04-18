package db

import ()

type User struct {
	Password  string
	Salt      string
	Balance   float64
	TwoFactor bool
	DeviceId  map[string]string
}

type Tables struct {
	Users map[string]User
}

func Init() *Tables {
	var DB Tables
	DB.Users = map[string]User{}
	return &DB
}

func (DB *Tables) CreateUser(userName, hashedPW, salt string, balance float64, twoFactor bool) {
	DB.Users[userName] = User{
		hashedPW,
		salt,
		balance,
		twoFactor,
		make(map[string]string),
	}
}
