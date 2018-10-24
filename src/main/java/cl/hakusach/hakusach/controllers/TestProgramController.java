package cl.hakusach.hakusach.controllers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.hakusach.hakusach.services.GlotService;
import cl.hakusach.hakusach.exceptions.LanguageNotSupported;
import cl.hakusach.hakusach.models.Languages;
import cl.hakusach.hakusach.requests.TestProgramRequest;
import cl.hakusach.hakusach.requests.GlotApiRequest;
import cl.hakusach.hakusach.requests.GlotApiRequest.FileReference;
import cl.hakusach.hakusach.responses.GlotApiResponse;

@RestController
@RequestMapping("/api/v1/test/")
public class TestProgramController{

    @Autowired
    private GlotService service;

    @PostMapping("program/")
    public GlotApiResponse testProgram(@RequestBody TestProgramRequest request) throws Exception {
        
        List<FileReference> programs = new ArrayList<>();

        switch(request.getLang()) {
            case Languages.C_LANG:
            case Languages.JAVA_LANG:
                throw new LanguageNotSupported();

            case Languages.PYTHON_LANG:
                // Program
                programs.add(FileReference.builder()
                    .name("main.py")
                    .content(request.getProgram())
                    .build()
                );
                break;

            default:
                throw new Exception("Bad Request::");
        }

        GlotApiRequest req = GlotApiRequest.builder()
                    .stdin(request.getStdin())
                    .files(programs)
                    .build();
        
        return service.sendProgram(req);
    }


}