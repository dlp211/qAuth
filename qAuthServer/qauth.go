package main

import (
  "fmt"
  "net/http"
  "bytes"
  "io/ioutil"
  "encoding/json"
  "encoding/gob"
  "log"
  "os"
 // "crypto/rsa"
)

/* logging functionality */
func precolor(color string) string {
  return "\033[" + color + "m"
}


func PANIC(str string) {
  log.SetPrefix(precolor("31;05;9") + "PANIC: ")
  log.Printf(str + "\033[0m")
}

func WARN(str string) {
  log.SetPrefix(precolor("38;05;9") + "WARN: ")
  log.Printf(str + "\033[0m")
}

func INFO(str string) {
  log.SetPrefix(precolor("38;05;2") + "INFO: ")
  log.Printf(str + "\033[0m")
}

func DEBUG(str string) {
  log.SetPrefix(precolor("38;05;4") + "DEBUG: ")
  log.Printf(str + "\033[0m")
}

/* STRUCTURES */
type watch_struct struct {
  WatchId string `json:"watchId"`
}

/* JSON Types */
type registration_t struct {
  UserName string `json:"email"`
  Password string `json:"password"`
  DeviceId string `json:"deviceId"`
  PKN string `json:"publicKey_n"`
  PKE int `json:"publicKey_e"`
}

type registerBT_t struct {
  UserName string `json:"email"`
  Password string `json:"password"`
  BluetoothId string `json:"bluetoothId"`
}

type provider_t struct {
  Provider string `json:"provider"`
  Key string `json:"key"`
  Package string `json:"package"`
  Callback string `json:"callback"`
}

type providerList_t struct {
  Packages []string `json:"packages"`
}

type publicKey_t struct {
  n string
  e int
}

/* DB Structs */
type DbEntry_t struct {
  password string
  deviceId []string
  bluetoothId []string
  pk publicKey_t
}

type DbProv_t struct {
  key string
  packageName string
  callback string
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

  INFO(fmt.Sprintf("response Status:", resp.Status))
  INFO(fmt.Sprintf("response Headers:", resp.Header))

  body, _ := ioutil.ReadAll(resp.Body)
  INFO(fmt.Sprintf("*ANDROID POST* response: ", string(body)))
}

/* Rest handler map */
var G_Rest = map[string]func(http.ResponseWriter, *http.Request){}

/* Maps as our DB tables */
var G_DB = map[string]DbEntry_t{}
var G_DB_PROVIDERS = map[string]DbProv_t{}

func init() {


  G_Rest["/gettoken"] =
  func (w http.ResponseWriter, r *http.Request) {
    var wS watch_struct
    err := json.NewDecoder(r.Body).Decode(&wS)
    if err != nil {
      panic(err)
    }

    if ( wS.WatchId == "007" ) {
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
  func (w http.ResponseWriter, r *http.Request) {
    INFO("/register")
    var reg registration_t
    err := json.NewDecoder(r.Body).Decode(&reg)
    if err != nil { panic( err ) }
    if _, ok := G_DB[reg.UserName]; ok {
      WARN("User " + reg.UserName + " attempted to reregister" )
      w.WriteHeader(http.StatusConflict)
    } else {
      //validate email
      G_DB[reg.UserName] = DbEntry_t{
        reg.Password,
        []string{reg.DeviceId},
        []string{},
        publicKey_t{reg.PKN, reg.PKE},
      }
      INFO("User " + reg.UserName + " successfully registered with deviceId: " + reg.DeviceId)
      w.WriteHeader(http.StatusAccepted)
    }
  }

  G_Rest["/register/bluetooth"] =
  func (w http.ResponseWriter, r *http.Request) {
    INFO("/register/bluetooth")
    var reg registerBT_t
    err := json.NewDecoder(r.Body).Decode(&reg)
    if err != nil { panic( err ) }
    if val, ok := G_DB[reg.UserName]; ok {
      if val.password != reg.Password {
        WARN("User " + reg.UserName + " failed to authenticate" )
        w.WriteHeader(http.StatusUnauthorized)
      } else {
        INFO("User " + reg.UserName + " registered BluetoothId: " + reg.BluetoothId )
        val.bluetoothId = append(val.bluetoothId, reg.BluetoothId)
        G_DB[reg.UserName] = val
        w.WriteHeader(http.StatusAccepted)
      }
    } else {
      INFO("User " + reg.UserName + " not found" )
      w.WriteHeader(http.StatusConflict)
    }
  }

  G_Rest["/register/device"] =
  func (w http.ResponseWriter, r *http.Request) {
    INFO("/register/device")
    var reg registration_t
    err := json.NewDecoder(r.Body).Decode(&reg)
    if err != nil { panic( err ) }
    if val, ok := G_DB[reg.UserName]; ok {
      if val.password != reg.Password {
        WARN("User " + reg.UserName + " failed to authenticate" )
        w.WriteHeader(http.StatusUnauthorized)
      } else {
        INFO("User " + reg.UserName + " registerer BluetoothId: " + reg.DeviceId )
        val.deviceId = append(val.deviceId, reg.DeviceId)
        G_DB[reg.UserName] = val
        w.WriteHeader(http.StatusAccepted)
      }
    } else {
      INFO("User " + reg.UserName + " not found" )
      w.WriteHeader(http.StatusConflict)
    }
  }

  G_Rest["/provider"] =
  func (w http.ResponseWriter, r *http.Request) {
    INFO("/provider")
    var prov provider_t
    err := json.NewDecoder(r.Body).Decode(&prov)
    if err != nil { panic( err ) }
    if _, ok := G_DB[prov.Provider]; ok {
      WARN("Provider " + prov.Provider + " attempted to reregister" )
      w.WriteHeader(http.StatusConflict)
    } else {
      G_DB_PROVIDERS[prov.Provider] = DbProv_t{
        prov.Key,
        prov.Package,
        prov.Callback,
      }
      INFO("Provider " + prov.Provider + " successfully registered with Key: " + prov.Key)
    }
  }

  G_Rest["/provider/available"] =
  func (w http.ResponseWriter, r *http.Request) {
    INFO("/provider/available")
    var list providerList_t
    for _,v := range G_DB_PROVIDERS {
      list.Packages = append(list.Packages, v.packageName)
    }
    writeOut, err := json.Marshal(list)
    if err != nil { panic(err) }
    w.Header().Set("Content-Type", "application/json")
    w.Write(writeOut)
  }

  G_Rest["/provider/activate"] =
  func (w http.ResponseWriter, r *http.Request) {
    fmt.Println()
  }

  G_Rest["/provider/deactivate"] =
  func (w http.ResponseWriter, r *http.Request) {
    fmt.Println()
  }

  G_Rest["/save"] =
  func (w http.ResponseWriter, r *http.Request) {
    data, err := os.Create("User_DB.gob")
    defer data.Close()
    if err != nil { panic(err) }
    data2, err2 := os.Create("Provider_DB.gob")
    defer data2.Close()
    if err2 != nil { panic(err2) }
    dataEncoder := gob.NewEncoder(data)
    dataEncoder.Encode(G_DB)
    dataEncoder = gob.NewEncoder(data2)
    dataEncoder.Encode(G_DB_PROVIDERS)
  }

  G_Rest["/load"] =
  func (w http.ResponseWriter, r *http.Request) {
    data, err := os.Open("User_DB.gob")
    defer data.Close()
    data2, err2 := os.Open("Provider_DB.gob")
    defer data2.Close()
    if err == nil {
      dataDecoder := gob.NewDecoder(data)
      err = dataDecoder.Decode(&G_DB)
      if err != nil { panic(err) }
    }
    if err2 == nil {
      dataDecoder := gob.NewDecoder(data2)
      err = dataDecoder.Decode(&G_DB_PROVIDERS)
      if err != nil { panic(err) }
    }
  }

}


func main() {

  fmt.Println("============================")
  fmt.Println("|| Starting Q-Auth Server ||")
  fmt.Println("============================")

  for point, function := range G_Rest {
    http.HandleFunc(point, function)
  }

  http.ListenAndServe(":8080", nil)
}


