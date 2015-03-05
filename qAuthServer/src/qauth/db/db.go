package db

import (
	"encoding/gob"
	"fmt"
	"logger"
	"os"
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

func (DB *Tables) CreateProvider(prov *model.RegisterProvider) {
	DB.Providers[prov.Provider] = Provider{
		prov.Key,
		prov.Package,
		prov.Callback,
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

func (DB *Tables) Save(file string) {
	logger.INFO("Invoking save on the DB")
	data, err := os.Create(file + ".gob")
	defer data.Close()
	if err != nil {
		panic(err)
	}
	data2, err2 := os.Create(file + "2.gob")
	defer data2.Close()
	if err2 != nil {
		panic(err2)
	}
	dataEncoder := gob.NewEncoder(data)
	dataEncoder.Encode(DB.Users)
	dataEncoder = gob.NewEncoder(data2)
	dataEncoder.Encode(DB.Providers)
	logger.INFO("Save complete")
}

func (DB *Tables) Load(file string) {
	logger.INFO("Attempting Load From DB")
	data, err := os.Open(file + ".gob")
	defer data.Close()
	data2, err2 := os.Open(file + "2.gob")
	defer data2.Close()
	if err == nil {
		logger.DEBUG("File data opened successfully")
		dataDecoder := gob.NewDecoder(data)
		err = dataDecoder.Decode(&DB.Users)
		if err != nil {
			logger.PANIC("Error decoding the Users table")
			panic(err)
		}
	}
	if err2 == nil {
		logger.DEBUG("File data2 opened successfully")
		dataDecoder := gob.NewDecoder(data2)
		err = dataDecoder.Decode(&DB.Providers)
		if err != nil {
			logger.PANIC("Error decoding the Providers table")
			panic(err)
		}
	}
}
