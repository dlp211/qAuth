package main

import (
    "fmt"      
    "net/http"
    "bytes"
    "io/ioutil"
    "encoding/json"
    "crypto/md5"
    "crypto/rand"
    "crypto/rsa"
    "math/big"
    "encoding/base64"
)

func main() {

  fmt.Println("============================")
  fmt.Println("|| Starting Q-Auth Server ||")
  fmt.Println("============================")

  http.HandleFunc("/whois", func(w http.ResponseWriter, r *http.Request) {
        //input := strings.SplitN(r.URL.Path, "/", 3)
        //fmt.Fprintf(w, "Testing route 1, %s", input[2] )
	
        //send gcm to clients for userId, client with watch will hit /gettoken
	sendGcmMessage()
	fmt.Println("/whois called, sent gcm message")
	fmt.Fprintf(w, "token007") //give token to dummyServer
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
          
	  //encrypt for client's public key
	  msg := []byte("token007")
	  label := []byte("")
	  md5hash := md5.New()

	  //var publickey rsa.PublicKey

	  nStr := "bd96a14af1dc72b14bd2864b4cde1023d3755fa071f68ac4619dfa7d9117985f4a1355069dffc100093945d945b4c1791a906034feeef95ebfc26b98161d0aecc6ceb785bec4a1ecd707db9b8877fef3f54b0b2d31cfe3e6053cb016dce28f7beee56d3d90da7bdee9668ce7ec3b635abe63b5c3594db500d3d157b7c399371f"

	  decN, err := base64.StdEncoding.DecodeString(nStr)
	  if err != nil {
	    fmt.Println(err)
	    return
	  }
	  n := big.NewInt(0)
	  n.SetBytes(decN)
	  publickey := rsa.PublicKey{N: n, E: 10001}
	  //var n int64
	  //buf := bytes.NewBuffer(byteModulus)
	  //binary.Read(buf, binary.LittleEndian, &n)

	  //bigint := big.NewInt(n)
	  //bigint, _ = bigint.SetString("bd96a14af1dc72b14bd2864b4cde1023d3755fa071f68ac4619dfa7d9117985f4a1355069dffc100093945d945b4c1791a906034feeef95ebfc26b98161d0aecc6ceb785bec4a1ecd707db9b8877fef3f54b0b2d31cfe3e6053cb016dce28f7beee56d3d90da7bdee9668ce7ec3b635abe63b5c3594db500d3d157b7c399371f", 1024)
	  //fmt.Println("big int: ", bigint.String() )
	  //publickey.N = bigint
	  //publickey.E = 10001
	  //publickey.N = bigFromString("bd96a14af1dc72b14bd2864b4cde1023d3755fa071f68ac4619dfa7d9117985f4a1355069dffc100093945d945b4c1791a906034feeef95ebfc26b98161d0aecc6ceb785bec4a1ecd707db9b8877fef3f54b0b2d31cfe3e6053cb016dce28f7beee56d3d90da7bdee9668ce7ec3b635abe63b5c3594db500d3d157b7c399371f")

	  encryptedmsg, err := rsa.EncryptOAEP(md5hash, rand.Reader, &publickey, msg, label)
	  if err != nil {
	    fmt.Println(err)
	  }
	  fmt.Println("encryptedmsg: ", string(encryptedmsg))
	  fmt.Fprintf(w, string(encryptedmsg) ) //return token for userId to qAuthClient
        } else {
          fmt.Fprintf(w, "false")
        }
	fmt.Println("/gettoken")
    })


  http.ListenAndServe(":8080", nil)

}

func bigFromString(s string) *big.Int {
  ret := new(big.Int)
  ret.SetString(s, 10)
  return ret
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

func sendGcmMessage() {
  url := "https://android.googleapis.com/gcm/send"

  //var jsonStr = []byte(`{"registration_ids":["APA91bEbel5v4Jl855Kx0pnQ-CkGbBGJEgKKrUbm8jQS-TfOM3hGOE2Y6AYY-_HXFQ4Jl37ErV095KmKa1VQ4UjhRDQBDqYdjBBcxcYRfX7rfDlMXGD2r6jxeDXQWTd6TUIFAsGYy6dBgCtTrXyPbBeGimHZzaB88A"]}`)
  //var jsonStr = []byte(`{"registration_ids":["APA91bHFrWxppzUzIlnxcSNdnChQauI6fGtDYdiT_8HwbpaYRsrZnvYPeavBpuk9ZqlvW5qlUSmJsbdeDq_nDpSTpDoTd86O3uzYtie_rURXHe_uGp3zwiI4jnW7CSg5RG1PhDMTD-WiSyEFrX7CYLii1Ln4atubjg"]}`)
  //var jsonStr = []byte(`{"registration_ids":["APA91bGiSgBQyZ_uXZlDq_7LY8j3yLz5snbn0llSFuxJDeh_qWPXSw-h7cuDRNoYjxSHeu93lj7uiz3cvwWGHCQV_7dBDap8fx3VS-ZikHG8pM4h4-tfO9flnP2Z3N5yfmbBaD6SFt32HSZnFO1jru7SaVPxPQHGzQ"]}`)
  var jsonStr = []byte(`{"registration_ids":["APA91bGiSgBQyZ_uXZlDq_7LY8j3yLz5snbn0llSFuxJDeh_qWPXSw-h7cuDRNoYjxSHeu93lj7uiz3cvwWGHCQV_7dBDap8fx3VS-ZikHG8pM4h4-tfO9flnP2Z3N5yfmbBaD6SFt32HSZnFO1jru7SaVPxPQHGzQ"]}`)

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

  //fmt.Println("response Status:", resp.Status)
  //fmt.Println("response Headers:", resp.Header)
  body, _ := ioutil.ReadAll(resp.Body)
  fmt.Println("*ANDROID POST* response: ", string(body))
}
