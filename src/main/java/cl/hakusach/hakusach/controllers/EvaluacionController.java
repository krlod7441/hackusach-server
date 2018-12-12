package cl.hakusach.hakusach.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.hakusach.hakusach.exceptions.AlumnoNotFound;
import cl.hakusach.hakusach.exceptions.EnunciadoNotFound;
import cl.hakusach.hakusach.exceptions.EvaluacionNotFound;
import cl.hakusach.hakusach.factory.Pipeline;
import cl.hakusach.hakusach.factory.PipelineFactory;
import cl.hakusach.hakusach.models.Alumno;
import cl.hakusach.hakusach.models.Enunciado;
import cl.hakusach.hakusach.models.Evaluacion;
import cl.hakusach.hakusach.models.Languages;
import cl.hakusach.hakusach.repositories.AlumnoRepository;
import cl.hakusach.hakusach.repositories.EnunciadoRepository;
import cl.hakusach.hakusach.repositories.EvaluacionRepository;
import cl.hakusach.hakusach.requests.GlotApiRequest;
import cl.hakusach.hakusach.requests.GlotApiRequest.FileReference;
import cl.hakusach.hakusach.responses.EvaluacionResult;
import cl.hakusach.hakusach.responses.GlotApiResponse;
import cl.hakusach.hakusach.services.GlotService;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/alumno")
public class EvaluacionController {

    @Autowired
    private AlumnoRepository alumnoRepository;

    @Autowired
    private EvaluacionRepository evaluacionRepository;

    @Autowired
    private EnunciadoRepository enunciadoRepository;

    @Autowired
    private GlotService service;

    @Autowired
    private PipelineFactory factory;

    private void ValidatePath(long idAlumno,  long idEnunciado) throws AlumnoNotFound, EnunciadoNotFound {
        Optional<Alumno> alumno =  alumnoRepository.findById(idAlumno);
        
        if(!alumno.isPresent()) {
            throw new AlumnoNotFound(idAlumno);
        }

        Optional<Enunciado> enunciado = enunciadoRepository.findById(idEnunciado);

        if(!enunciado.isPresent()) {
            throw new EnunciadoNotFound(idEnunciado);
        }


    }


    // Obtiene todas las evaluaciones de un alumno, para un enunciado
    @GetMapping("/{idAlumno}/enunciado/{idEnunciado}/evaluacion/all")
    public List<Evaluacion> getEvaluaciones(@PathVariable long idAlumno, @PathVariable long idEnunciado) throws Exception {
        ValidatePath(idAlumno, idEnunciado);

        List<Evaluacion> evaluaciones = evaluacionRepository.findByAlumnoIdAndEnunciadoId(idAlumno, idEnunciado);

        return evaluaciones;
    }

    // Crea una evaluacion para un alumno
    @PostMapping("/{idAlumno}/enunciado/{idEnunciado}/evaluacion/")
    public Evaluacion createEvaluacion(@PathVariable long idAlumno, @PathVariable long idEnunciado, @RequestBody Evaluacion evaluacion) throws Exception {
        ValidatePath(idAlumno, idEnunciado);

        Evaluacion nuevaEvaluacion = Evaluacion.builder()
            .alumnoId(idAlumno)
            .enunciadoId(idEnunciado)
            .code(evaluacion.getCode())
            .lang(evaluacion.getLang())
            .score(0)
            .build();
        
        nuevaEvaluacion =evaluacionRepository.save(nuevaEvaluacion);

        return nuevaEvaluacion;
    }
// Miercoles 12 de diciembre
    @PutMapping("/{idAlumno}/enunciado/{idEnunciado}/evaluacion/{idEvaluacion}")
    public Evaluacion updateEvaluacion(@PathVariable long idAlumno, @PathVariable long idEnunciado, @PathVariable long idEvaluacion, @RequestBody Evaluacion evaluacion) throws Exception {
        ValidatePath(idAlumno, idEnunciado);


        Optional<Evaluacion> result = evaluacionRepository.findById(idEvaluacion);

        if(!result.isPresent()) {
            throw new EvaluacionNotFound(idEvaluacion);
        }

        Evaluacion updateEvaluacion = result.get().toBuilder()
            .code(evaluacion.getCode())
            .build();

        updateEvaluacion = evaluacionRepository.save(updateEvaluacion);


        return updateEvaluacion;
    }

    @DeleteMapping("/{idAlumno}/enunciado/{idEnunciado}/evaluacion/{idEvaluacion}")
    public void delete(@PathVariable long idAlumno, @PathVariable long idEnunciado, @PathVariable long idEvaluacion) throws Exception {
        ValidatePath(idAlumno, idEnunciado);

        Optional<Evaluacion> result = evaluacionRepository.findById(idEvaluacion);

        if(!result.isPresent()) {
            throw new EvaluacionNotFound(idEvaluacion);
        }

        evaluacionRepository.delete(result.get());        
    }


    @PostMapping("/{idAlumno}/enunciado/{idEnunciado}/evaluacion/{idEvaluacion}/evaluate")
    public EvaluacionResult evaluate(@PathVariable long idAlumno, @PathVariable long idEnunciado, @PathVariable long idEvaluacion, @RequestBody Evaluacion evaluacion) throws Exception {
        ValidatePath(idAlumno, idEnunciado);


        Optional<Evaluacion> result = evaluacionRepository.findById(idEvaluacion);

        if(!result.isPresent()) {
            throw new EvaluacionNotFound(idEvaluacion);
        }

        Evaluacion updateEvaluacion = result.get().toBuilder()
            .code(evaluacion.getCode())
            .build();
            

        updateEvaluacion = evaluacionRepository.save(updateEvaluacion);
        
        // Enunciado & Test cases
        Optional<Enunciado> query = enunciadoRepository.findById(idEnunciado);

        Enunciado enunciado = query.get();
        List<String> entradas = enunciado.getEntradas();
        List<String> salidas = enunciado.getSalidas();

        int totalTest = entradas.size();
        String lang = updateEvaluacion.getLang();
        Languages langVar = null;
        String program = updateEvaluacion.getCode();
        String programName = "";

        if (lang.compareTo(Languages.C_LANG.getLanguage()) == 0) {
            programName = "main.c";
            langVar = Languages.C_LANG;
        }
        else if (lang.compareTo(Languages.JAVA_LANG.getLanguage()) ==0) {
            programName = "Main.java";
            langVar = Languages.JAVA_LANG;
        }
        else if (lang.compareTo(Languages.PYTHON_LANG.getLanguage()) == 0) {           
            programName = "main.py";
            langVar = Languages.PYTHON_LANG;
        }

        List<FileReference> programs = new ArrayList<>();
            programs.add(FileReference.builder()
                .name(programName)
                .content(program)
                .build());
        
        // Completable future
        List<CompletableFuture<GlotApiResponse>> tasks = new ArrayList<>();

        for(int i = 0; i < totalTest; i++) {
            tasks.add(service
                .assync()
                .sendProgram(
                    GlotApiRequest.builder()
                        .stdin(entradas.get(i))
                        .files(programs)
                        .build(),
                        langVar));
        }

        for(int i = 0; i < totalTest; i++) {
            CompletableFuture.allOf(tasks.get(i)).join();
        }
        List<EvaluacionResult.TestResult> testResults = new ArrayList<>();
        int testPassed = 0;
        for(int i = 0; i < totalTest; i++) {

            boolean passed = salidas.get(i).compareTo(tasks.get(i).get().getStdout()) == 0;
            if(passed) {
                testPassed++;
            }

            testResults.add(EvaluacionResult.TestResult.builder()
                                .input(entradas.get(i))
                                .output(tasks.get(i).get().getStdout())
                                .passed(passed)
                                .expected(salidas.get(i))
                                .build()
            );
        }

        // Update score
        int score = (testPassed / totalTest);
        updateEvaluacion = updateEvaluacion.toBuilder()
                                .score(score)
                                .build();
        
        updateEvaluacion = evaluacionRepository.save(updateEvaluacion);

        // Analize
        Pipeline pipeline = factory.createPipeline(lang);

        pipeline.analyze(program);

        return EvaluacionResult.builder()
            .results(testResults)
            .comments(pipeline.getCommentsResult())
            .identation(pipeline.getIdentationResult())
            .mainComments(pipeline.getMainCommentsResult())
            .variables(pipeline.getVariablesResult())
            .build();
    }




}
