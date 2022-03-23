package com.example.namingserver;




import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@org.springframework.web.bind.annotation.RestController
public class RestController {

    public RestController() {
    }

    @GetMapping(value = "/getietsnogveradneren/")
    public void getiets(){
        System.out.println("iets");
    }
}
