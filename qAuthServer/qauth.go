package main

import (
    "fmt"      
    "net/http"
    "bytes"
    "io/ioutil"
    "encoding/json"
)

func main() {

  fmt.Println("============================")
  fmt.Println("|| Starting Q-Auth Server ||")
  fmt.Println("============================")

  http.HandleFunc("/whois", func(w http.ResponseWriter, r *http.Request) {
        //input := strings.SplitN(r.URL.Path, "/", 3)
        //fmt.Fprintf(w, "Testing route 1, %s", input[2] )

        //send gcm to clients for userId, client with watch will hit /gettoken

    })

  http.HandleFunc("/gettoken", func(w http.ResponseWriter, r *http.Request) {
        //input := strings.SplitN(r.URL.Path, "/", 3)

	var wS watch_struct
        err := json.NewDecoder(r.Body).Decode(&wS)
        if err != nil {
          panic(err)
        }

        if ( wS.WatchId == "007" ){
          //get userId ... "1"
          sendToken("token007", "1")
          fmt.Fprintf(w, "token007") //return token for userId
        } else {
          fmt.Fprintf(w, "false")
        }

    })


  http.ListenAndServe(":8080", nil)

}

type watch_struct struct {
    WatchId string `json:"watchId"`
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
