package controller

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"logger"
	"net/http"
	"qauth/authenticate"
	"qauth/db"
	"qauth/model"
)

var Controllers = map[string]func(http.ResponseWriter, *http.Request){}
var DB *db.Tables

/* USER REGISTRATION CONTROLLERS */
func Register(w http.ResponseWriter, r *http.Request) {
	logger.INFO("/register")
	var reg model.Registration
	err := reg.Decode(r.Body)
	if err != nil {
		panic(err)
	}

	if _, ok := DB.Users[reg.UserName]; ok {
		logger.WARN("User " + reg.UserName + " attempted to reregister")
		w.WriteHeader(http.StatusConflict)
	} else {
		hashedPW, salt := authenticate.NewPWHash(reg.Password)
		DB.CreateUser(&reg, hashedPW, salt)
		logger.INFO("User " + reg.UserName + " successfully registered with deviceId: " + reg.DeviceId + "and GCM ID: " + reg.GCMId)
		w.WriteHeader(http.StatusAccepted)
	}
}

func RegisterBluetoothID(w http.ResponseWriter, r *http.Request) {
	logger.INFO("/register/bluetooth")
	var reg model.RegisterBT
	err := reg.Decode(r.Body)
	if err != nil {
		panic(err)
	}

	if val, ok := DB.Users[reg.UserName]; ok {
		if authenticate.Password(reg.Password, val.Salt, val.Password) {
			logger.WARN("User " + reg.UserName + " failed to authenticate")
			w.WriteHeader(http.StatusUnauthorized)
		} else {
			logger.INFO("User " + reg.UserName + " registered BluetoothId: " + reg.BluetoothId)
			val.BluetoothId = reg.BluetoothId
			val.Pk = model.PublicKey{reg.PKN, reg.PKE}
			if DB.UpdateUser(reg.UserName, &val) {
				w.WriteHeader(http.StatusAccepted)
			} else {
				w.WriteHeader(http.StatusInternalServerError)
			}
		}
	} else {
		logger.INFO("User " + reg.UserName + " not found")
		w.WriteHeader(http.StatusConflict)
	}
}

/* USER UPDATE CONTROLLERS */ /*
//THIS REQUIRES THAT WE CAN ENCRYPT AND DECRYPT
func AddDevice(w http.ResponseWriter, r *http.Request) {
	logger.INFO("/register/device")
	var reg registration_t
	err := json.NewDecoder(r.Body).Decode(&reg)
	if err != nil {
		panic(err)
	}
	if val, ok := G_DB[reg.UserName]; ok {
		if AUTHENTICATE(reg.Password, val.salt, val.password) {
			logger.WARN("User " + reg.UserName + " failed to authenticate")
			w.WriteHeader(http.StatusUnauthorized)
		} else {
			logger.INFO("User " + reg.UserName + " registerer BluetoothId: " + reg.DeviceId)
			val.deviceId = append(val.deviceId, reg.DeviceId)
			G_DB[reg.UserName] = val
			w.WriteHeader(http.StatusAccepted)
		}
	} else {
		logger.INFO("User " + reg.UserName + " not found")
		w.WriteHeader(http.StatusConflict)
	}
}

/* PROVIDER REGISTRATION CONTROLLERS */
func AddProvider(w http.ResponseWriter, r *http.Request) {
	logger.INFO("/provider")
	var provider model.RegisterProvider
	err := provider.Decode(r.Body)
	if err != nil {
		panic(err)
	}
	if _, ok := DB.Providers[provider.Provider]; ok {
		logger.WARN("Provider " + provider.Provider + " attempted to reregister")
		w.WriteHeader(http.StatusConflict)
	} else {
		DB.CreateProvider(&provider)
		logger.INFO("Provider " + provider.Provider + " successfully registered with Key: " + provider.Key)
	}
}

/* MANAGE PACKAGE SET CONTROLLERS */
func GetAllAvailablePackages(w http.ResponseWriter, r *http.Request) {
	logger.INFO("/provider/available")
	var packages model.PackageList
	for k, _ := range DB.Providers {
		packages.Packages = append(packages.Packages, k)
	}
	writeOut, err := json.Marshal(packages)
	if err != nil {
		panic(err)
	}
	w.Header().Set("Content-Type", "application/json")
	w.Write(writeOut)
}

func AddUserToProvider(w http.ResponseWriter, r *http.Request) {
	logger.INFO("/provider/activate")
	var activate model.AddPackage
	err := activate.Decode(r.Body)
	if err != nil {
		panic(err)
	}
	if val, ok := DB.Providers[activate.Package]; ok {
		logger.INFO("User " + activate.UserName + " activated Package: " + activate.Package)
		val.Users[activate.ProviderUserName] = activate.UserName
		if DB.UpdateProvider(activate.Package, &val) {
			w.WriteHeader(http.StatusAccepted)
		} else {
			w.WriteHeader(http.StatusInternalServerError)
		}
	} else {
		logger.INFO("Package " + activate.Package + " not found")
		w.WriteHeader(http.StatusConflict)
	}
}

func DeleteUserFromProvider(w http.ResponseWriter, r *http.Request) {
	logger.INFO("/provider/deactivate")
	var activate model.AddPackage
	err := activate.Decode(r.Body)
	if err != nil {
		panic(err)
	}
	if val, ok := DB.Providers[activate.Package]; ok {
		logger.INFO("User " + activate.UserName + " deactivated Package: " + activate.Package)
		delete(val.Users, activate.ProviderUserName)
	} else {
		logger.INFO("Package " + activate.Package + " not found")
		w.WriteHeader(http.StatusConflict)
	}
}

/* ADMIN CONTROLLERS */
func SaveDBsToFile(w http.ResponseWriter, r *http.Request) {
	logger.INFO("/db/save")
	var admin model.AdminDBAccess
	err := admin.Decode(r.Body)
	if err != nil {
		panic(err)
	}
	if authenticate.AdminAuth(admin.Key) {
		DB.Save(admin.File)
		w.WriteHeader(http.StatusOK)
	} else {
		logger.WARN("Unauthorized attempt to save DB's with key: " + admin.Key)
		w.WriteHeader(http.StatusUnauthorized)
	}
}

func LoadDBsFromFile(w http.ResponseWriter, r *http.Request) {
	logger.INFO("/db/load")
	var admin model.AdminDBAccess
	err := admin.Decode(r.Body)
	if err != nil {
		panic(err)
	}
	if authenticate.AdminAuth(admin.Key) {
		DB.Load(admin.File)
		w.WriteHeader(http.StatusOK)
	} else {
		logger.WARN("Unauthorized attempt to save DB's with key: " + admin.Key)
		w.WriteHeader(http.StatusUnauthorized)
	}
}

func DisplayUserDB(w http.ResponseWriter, r *http.Request) {
	logger.INFO("/db/show/users")
	var admin model.AdminDBAccess
	err := admin.Decode(r.Body)
	if err != nil {
		panic(err)
	}
	if authenticate.AdminAuth(admin.Key) {
		for key, value := range DB.Users {
			fmt.Println(key, value.String())
		}
		w.WriteHeader(http.StatusOK)
	} else {
		logger.WARN("ATTEMPT TO DUMP DB TABLE USERS DENIED")
		w.WriteHeader(http.StatusUnauthorized)
	}
}

func DisplayProviderDB(w http.ResponseWriter, r *http.Request) {
	logger.INFO("/db/show/providers")
	var admin model.AdminDBAccess
	err := admin.Decode(r.Body)
	if err != nil {
		panic(err)
	}
	if authenticate.AdminAuth(admin.Key) {
		for key, value := range DB.Providers {
			fmt.Println(key, value.String())
		}
		w.WriteHeader(http.StatusOK)
	} else {
		logger.WARN("ATTEMPT TO DUMP DB TABLE PROVIDERS DENIED")
		w.WriteHeader(http.StatusUnauthorized)
	}
}

func TestRSA(w http.ResponseWriter, r *http.Request) {
	logger.INFO("/test")
	var payload model.TestPayload
	err := payload.Decode(r.Body)
	if err != nil {
		panic(err)
	}
	str := authenticate.Decrypt(payload.Payload)
	logger.INFO("DECYPTED TEXT: " + str)
	w.WriteHeader(http.StatusOK)
}

func AttemptAuthenticate(w http.ResponseWriter, r *http.Request) {
	logger.INFO("/authenticate")
	var auth model.ServiceRequest
	err := auth.Decode(r.Body)
	if err != nil {
		panic(err)
	}
	logger.DEBUG("HERE")
	if user, ok := DB.Users[auth.Username]; ok {
		sendGcmMessage(user.GCMId[0]) //fixme need to change to a map
		w.WriteHeader(http.StatusOK)
	} else {
		w.WriteHeader(http.StatusUnauthorized)
	}
}

func sendGcmMessage(gcmid string) {
	url := "https://android.googleapis.com/gcm/send"

	var jsonStr = []byte(`{"registration_ids":["` + gcmid + `"]}`)

	req, err := http.NewRequest("POST", url, bytes.NewBuffer(jsonStr))
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("project_id", "156110196668")
	req.Header.Set("Authorization", "key=AIzaSyAFqyh9ZZFiY8HRcyUlidAg7IT3rvoN-Pk")

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		panic(err)
	}
	defer resp.Body.Close()

	body, _ := ioutil.ReadAll(resp.Body)
	logger.DEBUG("response: " + string(body))
}

func BuildControllerSet() {
	Controllers["/register"] = Register
	Controllers["/register/bluetooth"] = RegisterBluetoothID
	//Controllers["/register/device"] = AddDevice

	Controllers["/provider"] = AddProvider
	Controllers["/provider/available"] = GetAllAvailablePackages
	Controllers["/provider/activate"] = AddUserToProvider
	Controllers["/provider/deactivate"] = DeleteUserFromProvider

	Controllers["/db/save"] = SaveDBsToFile
	Controllers["/db/load"] = LoadDBsFromFile
	Controllers["/db/show/users"] = DisplayUserDB
	Controllers["/db/show/providers"] = DisplayProviderDB

	Controllers["/authenticate"] = AttemptAuthenticate

	Controllers["/test"] = TestRSA
}
