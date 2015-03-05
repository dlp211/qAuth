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
	Key          string
	ProviderName string
	Callback     string
	Users        map[string]string
	Pk           model.PublicKey
}

func (p *Provider) String() string {
	return fmt.Sprintf("KEY: %s\tPACKAGE: %s\tCALLBACK_URL: %s", p.Key, p.ProviderName, p.Callback)
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
	DB.Providers[prov.Package] = Provider{
		prov.Key,
		prov.Provider,
		prov.Callback,
		make(map[string]string),
		model.PublicKey{"", 0},
	}
}

func (DB *Tables) UpdateUser(name string, user *User) bool {
	if _, ok := DB.Users[name]; ok {
		DB.Users[name] = *user
		return true
	}
	return false
}

func (DB *Tables) UpdateProvider(pkg string, prov *Provider) bool {
	if _, ok := DB.Providers[pkg]; ok {
		DB.Providers[pkg] = *prov
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
	dataEncoder := gob.NewEncoder(data)
	dataEncoder.Encode(DB.Users)

	data2, err2 := os.Create(file + "2.gob")
	defer data2.Close()
	if err2 != nil {
		panic(err2)
	}
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
