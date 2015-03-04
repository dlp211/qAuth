package controller

import (
	"fmt"
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
		//validate email
		hashedPW, salt := authenticate.NewPWHash(reg.Password)
		DB.CreateUser(&reg, hashedPW, salt)
		logger.INFO("User " + reg.UserName + " successfully registered with deviceId: " + reg.DeviceId)
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

/* PROVIDER REGISTRATION CONTROLLERS */ /*
func AddProvider(w http.ResponseWriter, r *http.Request) {
	logger.INFO("/provider")
	var prov provider_t
	err := json.NewDecoder(r.Body).Decode(&prov)
	if err != nil {
		panic(err)
	}
	if _, ok := G_DB[prov.Provider]; ok {
		logger.WARN("Provider " + prov.Provider + " attempted to reregister")
		w.WriteHeader(http.StatusConflict)
	} else {
		G_DB_PROVIDERS[prov.Provider] = DbProv_t{
			prov.Key,
			prov.Package,
			prov.Callback,
		}
		logger.INFO("Provider " + prov.Provider + " successfully registered with Key: " + prov.Key)
	}
}

/* MANAGE PACKAGE SET CONTROLLERS */ /*
func GetAllAvailablePackages(w http.ResponseWriter, r *http.Request) {
	logger.INFO("/provider/available")
	var list providerList_t
	for _, v := range G_DB_PROVIDERS {
		list.Packages = append(list.Packages, v.packageName)
	}
	writeOut, err := json.Marshal(list)
	if err != nil {
		panic(err)
	}
	w.Header().Set("Content-Type", "application/json")
	w.Write(writeOut)
}

func AddProviderToUser(w http.ResponseWriter, r *http.Request) {
	fmt.Println()
}

func DeleteProviderFromUser(w http.ResponseWriter, r *http.Request) {
	fmt.Println()
}

/* ADMIN CONTROLLERS */ /*
func SaveDBsToFile(w http.ResponseWriter, r *http.Request) {
	logger.INFO("/save")
	var acc dbAccess_t
	err := json.NewDecoder(r.Body).Decode(&acc)
	if err != nil {
		panic(err)
	}
	if acc.Key == AdminKey {
		logger.INFO("Invoking save on the DB's")
		data, err := os.Create(acc.File + ".gob")
		defer data.Close()
		if err != nil {
			panic(err)
		}
		data2, err2 := os.Create(acc.File + "2.gob")
		defer data2.Close()
		if err2 != nil {
			panic(err2)
		}
		dataEncoder := gob.NewEncoder(data)
		dataEncoder.Encode(G_DB)
		dataEncoder = gob.NewEncoder(data2)
		dataEncoder.Encode(G_DB_PROVIDERS)
		logger.INFO("Save complete")
		w.WriteHeader(http.StatusOK)
	} else {
		logger.WARN("Unauthorized attempt to save DB's with key: " + acc.Key)
		w.WriteHeader(http.StatusUnauthorized)
	}
}

func LoadDBsFromFile(w http.ResponseWriter, r *http.Request) {
	logger.INFO("/load")
	var acc dbAccess_t
	err := json.NewDecoder(r.Body).Decode(&acc)
	if err != nil {
		panic(err)
	}
	if acc.Key == AdminKey {
		data, err := os.Open(acc.File + ".gob")
		defer data.Close()
		data2, err2 := os.Open(acc.File + "2.gob")
		defer data2.Close()
		if err == nil {
			dataDecoder := gob.NewDecoder(data)
			err = dataDecoder.Decode(&G_DB)
			if err != nil {
				panic(err)
			}
		}
		if err2 == nil {
			dataDecoder := gob.NewDecoder(data2)
			err = dataDecoder.Decode(&G_DB_PROVIDERS)
			if err != nil {
				panic(err)
			}
		}
	} else {
		logger.WARN("Unauthorized attempt to save DB's with key: " + acc.Key)
		w.WriteHeader(http.StatusUnauthorized)
	}
}
*/

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
		w.WriteHeader(http.StatusUnauthorized)
	}
}

func BuildControllerSet() {
	Controllers["/register"] = Register
	//Controllers["/register/bluetooth"] = RegisterBluetoothID
	//Controllers["/register/device"] = AddDevice

	//Controllers["/provider"] = AddProvider
	//Controllers["/provider/available"] = GetAllAvailablePackages
	//Controllers["/provider/activate"] = AddProviderToUser
	//Controllers["/provider/deactivate"] = DeleteProviderFromUser

	//Controllers["/db/save"] = SaveDBsToFile
	//Controllers["/db/load"] = LoadDBsFromFile
	Controllers["/db/show/users"] = DisplayUserDB
}

/*


func sendToken(token string, userId string) { //eventually support sending to multiple servers
	url := "http://107.170.156.222:8081/givetoken"

	var jsonStr = []byte(`{"token":"token007","userId":"1"}`)
	req, err := http.NewRequest("POST", url, bytes.NewBuffer(jsonStr))
	req.Header.Set("Content-Type", "application/json")

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		panic(err)
	}
	defer resp.Body.Close()

	logger.INFO(fmt.Sprintf("response Status:", resp.Status))
	logger.INFO(fmt.Sprintf("response Headers:", resp.Header))

	body, _ := ioutil.ReadAll(resp.Body)
	logger.INFO(fmt.Sprintf("*ANDROID POST* response: ", string(body)))
}


	G_Rest["/gettoken"] =
		func(w http.ResponseWriter, r *http.Request) {
			var wS watch_struct
			err := json.NewDecoder(r.Body).Decode(&wS)
			if err != nil {
				panic(err)
			}

			if wS.WatchId == "007" {
				//get userId ... "1"
				sendToken("token007", "1")
				fmt.Fprintf(w, "token007") //return token for userId
			} else {
				fmt.Fprintf(w, "false")
			}
		}

	G_Rest["/whois"] =
		func(w http.ResponseWriter, r *http.Request) {
			//input := strings.SplitN(r.URL.Path, "/", 3)
			//fmt.Fprintf(w, "Testing route 1, %s", input[2] )

			//send gcm to clients for userId, client with watch will hit /gettoken

		}
*/
