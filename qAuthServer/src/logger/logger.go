package logger

import (
  "log"
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
