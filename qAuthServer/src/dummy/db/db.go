package db

import ()

type User struct {
	Password  string
	Salt      string
	GPA       float32
	TwoFactor bool
}

type Tables struct {
	Users map[string]User
}

func Init() *Tables {
	var DB Tables
	DB.Users = map[string]User{}
	return &DB
}

func (DB *Tables) CreateUser(userName, hashedPW, salt string, gpa float32, twoFactor bool) {
	DB.Users[userName] = User{
		hashedPW,
		salt,
		gpa,
		twoFactor,
	}
}
