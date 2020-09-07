import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.StructureDefinition;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class FhirValidationApplication {


    private static Observation getObservationFromFile(FhirContext ctx) throws FileNotFoundException {
        Path currentRelativePath = Paths.get("");
        String basePath = currentRelativePath.toAbsolutePath().toString();
        String inputJson = basePath + "/src/input-data/labObservationCorrect.json";
        IParser parser = ctx.newJsonParser();
        Observation obs = parser.parseResource(Observation.class, new FileReader(inputJson));
        System.out.println("basePath = " +inputJson);
        return obs;
    }

    private static StructureDefinition getStructureDefinitonFromFile(FhirContext ctx) throws FileNotFoundException {
        Path currentRelativePath = Paths.get("");
        String basePath = currentRelativePath.toAbsolutePath().toString();
        String inputJson = basePath + "/src/profiles/labObsStruct.json";
        IParser parser = ctx.newJsonParser();
        StructureDefinition myStruct = parser.parseResource(StructureDefinition.class, new FileReader(inputJson));
        return myStruct;
    }

    public static void main(String[] args) throws FileNotFoundException {

        FhirContext ctx = FhirContext.forR4();

        // Get Observation from file
        Observation obs = getObservationFromFile(ctx);

        //Create Validation support
        PrePopulatedValidationSupport prepop = new PrePopulatedValidationSupport(ctx);
        prepop.addStructureDefinition(getStructureDefinitonFromFile(ctx));

        ValidationSupportChain validationSupportChain =
                new ValidationSupportChain(
                        new DefaultProfileValidationSupport(ctx),
                        prepop,
                        new InMemoryTerminologyServerValidationSupport(ctx),
                        new CommonCodeSystemsTerminologyService(ctx));


        // Create Validator
        FhirInstanceValidator instanceValidator = new FhirInstanceValidator(validationSupportChain);
        FhirValidator validator = ctx.newValidator();
        validator.registerValidatorModule(instanceValidator);


        //ValidationOptions valOpts = new ValidationOptions()
        //valOpts.addProfile("https://simplifier.net/packages/de.medizininformatikinitiative.kerndatensatz.laborbefund/1.0.1/files/147497");

        ValidationResult result =
                validator.validateWithResult(obs);

        List<SingleValidationMessage> valResultMessages = result.getMessages();
        boolean isValidFhir = result.isSuccessful();
        for (SingleValidationMessage message : valResultMessages) {
            System.out.println(
                    "issue: "
                            + message.getSeverity()
                            + " - "
                            + message.getLocationString()
                            + " - "
                            + message.getMessage());
        }


    }
}
