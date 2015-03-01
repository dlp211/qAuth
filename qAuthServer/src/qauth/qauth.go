package main

import (
	"bytes"
	"encoding/gob"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"logger"
	"net/http"
	"os"
	// "crypto/rsa"
	"crypto/rand"
	"crypto/sha1"
)

/* STRUCTURES */
type watch_struct struct {
	WatchId string `json:"watchId"`
}

/* JSON Types */
type registration_t struct {
	UserName string `json:"email"`
	Password string `json:"password"`
	DeviceId string `json:"deviceId"`
	PKN      string `json:"publicKey_n"`
	PKE      int    `json:"publicKey_e"`
}

type registerBT_t struct {
	UserName    string `json:"email"`
	Password    string `json:"password"`
	BluetoothId string `json:"bluetoothId"`
}

type provider_t struct {
	Provider string `json:"provider"`
	Key      string `json:"key"`
	Package  string `json:"package"`
	Callback string `json:"callback"`
}

type providerList_t struct {
	Packages []string `json:"packages"`
}

type dbAccess_t struct {
	File string `json:"file"`
	Key  string `json:"key"`
}

type publicKey_t struct {
	n string
	e int
}

/* DB Structs */
type DbEntry_t struct {
	password    string
	salt        string
	deviceId    []string
	bluetoothId []string
	pk          publicKey_t
}

type DbProv_t struct {
	key         string
	packageName string
	callback    string
}

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

func AUTHENTICATE(password, salt, hash string) bool {
	hasher := sha1.New()
	hasher.Write([]byte(password + salt))
	return fmt.Sprintf("% x", hasher.Sum(nil)) == hash
}

func generateHash(password string) (string, string) {
	b := make([]byte, 64)
	_, err := rand.Read(b)
	if err != nil {
		panic(err)
	}
	salt := string(b)
	hasher := sha1.New()
	hasher.Write([]byte(password + salt))
	return fmt.Sprintf("% x", hasher.Sum(nil)), salt
}

/* Rest handler map */
var G_Rest = map[string]func(http.ResponseWriter, *http.Request){}

/* Maps as our DB tables */
var Users = map[string]DbEntry_t{}
var Providers = map[string]DbProv_t{}

var AdminKey string

func init() {

	AdminKey = "AAAA" /* This will be changed to pull from an ENV variable */

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

	G_Rest["/register"] =
		func(w http.ResponseWriter, r *http.Request) {
			logger.INFO("/register")
			var reg registration_t
			err := json.NewDecoder(r.Body).Decode(&reg)
			if err != nil {
				panic(err)
			}
			if _, ok := G_DB[reg.UserName]; ok {
				logger.WARN("User " + reg.UserName + " attempted to reregister")
				w.WriteHeader(http.StatusConflict)
			} else {
				//validate email
				hashedPW, salt := generateHash(reg.Password)
				G_DB[reg.UserName] = DbEntry_t{
					hashedPW,
					salt,
					[]string{reg.DeviceId},
					[]string{},
					publicKey_t{reg.PKN, reg.PKE},
				}
				logger.INFO("User " + reg.UserName + " successfully registered with deviceId: " + reg.DeviceId)
				w.WriteHeader(http.StatusAccepted)
			}
		}

	G_Rest["/register/bluetooth"] =
		func(w http.ResponseWriter, r *http.Request) {
			logger.INFO("/register/bluetooth")
			var reg registerBT_t
			err := json.NewDecoder(r.Body).Decode(&reg)
			if err != nil {
				panic(err)
			}
			if val, ok := G_DB[reg.UserName]; ok {
				if AUTHENTICATE(reg.Password, val.salt, val.password) {
					logger.WARN("User " + reg.UserName + " failed to authenticate")
					w.WriteHeader(http.StatusUnauthorized)
				} else {
					logger.INFO("User " + reg.UserName + " registered BluetoothId: " + reg.BluetoothId)
					val.bluetoothId = append(val.bluetoothId, reg.BluetoothId)
					G_DB[reg.UserName] = val
					w.WriteHeader(http.StatusAccepted)
				}
			} else {
				logger.INFO("User " + reg.UserName + " not found")
				w.WriteHeader(http.StatusConflict)
			}
		}

	G_Rest["/register/device"] =
		func(w http.ResponseWriter, r *http.Request) {
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

	G_Rest["/provider"] =
		func(w http.ResponseWriter, r *http.Request) {
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

	G_Rest["/provider/available"] =
		func(w http.ResponseWriter, r *http.Request) {
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

	G_Rest["/provider/activate"] =
		func(w http.ResponseWriter, r *http.Request) {
			fmt.Println()
		}

	G_Rest["/provider/deactivate"] =
		func(w http.ResponseWriter, r *http.Request) {
			fmt.Println()
		}

	G_Rest["/save"] =
		func(w http.ResponseWriter, r *http.Request) {
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

	G_Rest["/load"] =
		func(w http.ResponseWriter, r *http.Request) {
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
}

func main() {

	fmt.Println("============================")
	fmt.Println("|| Starting Q-Auth Server ||")
	fmt.Println("============================")

	fmt.Println("LOGGER TESTS")
	logger.INFO("INFO")
	logger.DEBUG("DEBUG")
	logger.WARN("WARN")
	logger.PANIC("PANIC")

	for point, function := range G_Rest {
		http.HandleFunc(point, function)
	}

	http.ListenAndServe(":8080", nil)
}
