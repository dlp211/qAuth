package main

import (
  "fmt"
  "net/http"
  "bytes"
  "io/ioutil"
  "encoding/json"
)

/* STRUCTURES */
type watch_struct struct {
  WatchId string `json:"watchId"`
}

type registration_t struct {
  UserName string `json:"userName"`
  Password string `json:"password"`
  DeviceId string `json:"deviceId"`
}

type DbEntry_t struct {
  password string
  deviceID []string
}

/* Rest handler map */
var G_Rest = map[string]func(http.ResponseWriter, *http.Request){}

/* Map as our DB */
var G_DB = map[string]DbEntry_t{}

func init() {
  G_Rest["/gettoken"] = func (w http.ResponseWriter, r *http.Request) {
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

  G_Rest["/whois"] = func(w http.ResponseWriter, r *http.Request) {
    //input := strings.SplitN(r.URL.Path, "/", 3)
    //fmt.Fprintf(w, "Testing route 1, %s", input[2] )

    //send gcm to clients for userId, client with watch will hit /gettoken

  }

  G_Rest["/registration"] = func (w http.ResponseWriter, r *http.Request) {
    var reg registration_t
    err := json.NewDecoder(r.Body).Decode(&reg)
    if err != nil { panic( err ) }
    if _, ok := G_DB[reg.UserName]; ok {
      w.WriteHeader(http.StatusConflict)
    } else {
      w.WriteHeader(http.StatusAccepted)
      G_DB[reg.UserName] = DbEntry_t{ reg.Password, []string{reg.DeviceId} }
    }
    for k,v := range G_DB {
      fmt.Println(k , v)
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


func sendToken(token string, userId string) { //eventually support sending to multiple servers
  url := "http://107.170.156.222:8081/givetoken"
  //fmt.Println("URL:>", url)

  var jsonStr = []byte(`{"token":"token007","userId":"1"}`)
  req, err := http.NewRequest("POST", url, bytes.NewBuffer(jsonStr))
  req.Header.Set("Content-Type", "application/json")

  client := &http.Client{}
  resp, err := client.Do(req)
  if err != nil {
    panic(err)
  }
  defer resp.Body.Close()

  //fmt.Println("response Status:", resp.Status)
  //fmt.Println("response Headers:", resp.Header)
  body, _ := ioutil.ReadAll(resp.Body)
  fmt.Println("*ANDROID POST* response: ", string(body))
}
