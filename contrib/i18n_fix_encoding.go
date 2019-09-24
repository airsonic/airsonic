package main

// Recompile the script using 'go build i18n_fix_encoding.go'
// Script needs to be executed inside the airsonic-main/src/main/resources/org/airsonic/player/i18n/
// folder.

// Unicode range could be changed to something wider

import (
	"errors"
	"fmt"
	"io/ioutil"
	"log"
	"os"
	"path/filepath"
	"regexp"
	"strings"
)

const i18nFilesPath = "airsonic-main/src/main/resources/org/airsonic/player/i18n"

func main() {
	dir, err := os.Getwd()
	if err != nil {
		log.Fatal(err)
	}

	if !strings.Contains(dir, i18nFilesPath) {
		log.Fatal(errors.New("This script needs to be run inside the " + i18nFilesPath + " folder"))
	}

	fmt.Println(dir)
	// List all files in current dir
	files, err := filepath.Glob("*.properties")
	if err != nil {
		log.Fatal(err)
	}

	// Iterate over files
	for _, path := range files {
		f, err := os.Open(path)
		if err != nil {
			log.Fatal(err)
		}

		// Read file data
		data, err := ioutil.ReadAll(f)
		if err != nil {
			log.Fatal(err)
		}

		p := regexp.MustCompile("[\u0080-\u00ff]+")

		// Replace any found char by its unicode representation
		out := p.ReplaceAllStringFunc(string(data), func(in string) string {
			return "\\u" + strings.ToLower(strings.TrimLeft(fmt.Sprintf("%U", []rune(in)[0]), "U+"))
		})

		// If any changes where made write them to file
		if string(data) != out {
			fmt.Printf("Changes where made for %s\n", path)
			if err := ioutil.WriteFile(path, []byte(out), 0644); err != nil {
				log.Fatal(err)
			}
		}
	}
}
