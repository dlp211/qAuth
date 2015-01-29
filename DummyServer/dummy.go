package main

import (
    "encoding/json"
    "fmt"
    "io/ioutil"      
    "net/http"
    "strings"
    "bytes"
    "log"
)

func main() {

  fmt.Println("===========================")
  fmt.Println("|| Starting Dummy Server ||")
  fmt.Println("===========================")

  http.HandleFunc("/route1/", func(w http.ResponseWriter, r *http.Request) {
        input := strings.SplitN(r.URL.Path, "/", 3)
        fmt.Fprintf(w, "dummy server reporting for duty, %s", input[2] )

        //fmt.Fprintf(w, auth("testing") )
        dummyPost()
    })

  http.HandleFunc("/login/", func(w http.ResponseWriter, r *http.Request) {

	//token := strings.SplitN(r.URL.Path, "/", 3)
        //fmt.Fprintf(w, "login, token: %s", token[2] )
	
        var t test_struct   
	err := json.NewDecoder(r.Body).Decode(&t)
	log.Println(t)
	log.Println(t.Username)
	
        if err != nil {
        log.Println(err)
	 panic(err)
        }
        //fmt.Fprintf(w, "\nUsername: " + t.Username + " | Password: " + t.Password + "\n" )

        if ( t.Username == "davemayne" ) && ( t.Password == "test" ){
            fmt.Fprintf(w, "true")
        } else {
	    fmt.Fprintf(w, "false")
	}
        
    })
  http.HandleFunc("/authme", func(w http.ResponseWriter, r *http.Request){
	
	var t token_struct
	err := json.NewDecoder(r.Body).Decode(&t)
	if err != nil {
	  panic(err)
	}
	
	//check if token is a temp in static data structure
	if ( t.Token == "token007" ) && ( t.UserId == "1" ){
	  fmt.Fprintf(w, "true")
	} else {
	  fmt.Fprintf(w, "false")
	}

  })

  http.HandleFunc("/givetoken", func(w http.ResponseWriter, r *http.Request){
	
	var t token_struct
	err := json.NewDecoder(r.Body).Decode(&t)
	if err != nil {
	  panic(err)
	}
	
	//add token as a temp to a static data structure bc it should get used soon
	if ( t.Token == "token007" ) && ( t.UserId == "1" ){
	  fmt.Fprintf(w, "true")
	} else {
	  fmt.Fprintf(w, "false")
	}
	log.Println("Received token: " + t.Token + " for user id: " + t.UserId + " from qAuth Server")
  })

  http.ListenAndServe(":8081", nil)

}

type token_struct struct {
    Token string `json:"token"`
    UserId string `json:"userId"`
}

type test_struct struct {
    Username string `json:"username"`
    Password string `json:"password"`
}

func auth(token string) string { 

  url := "http://localhost:8080/route1/" + token;

  resp, err := http.Get(url)
  fmt.Println(err)
  defer resp.Body.Close()
  body, err := ioutil.ReadAll(resp.Body)

  fmt.Println("Q-Auth response: %s", string(body) ) 
  
  return "\n" + string(body) + "\n"
}

func dummyPost() {
  url := "http://107.170.156.222:8081/login/testToken123"
  //fmt.Println("URL:>", url)

  var jsonStr = []byte(`{"username":"davemayne","password":"test"}`)
  req, err := http.NewRequest("POST", url, bytes.NewBuffer(jsonStr))
  //req.Header.Set("X-Custom-Header", "myvalue")
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

