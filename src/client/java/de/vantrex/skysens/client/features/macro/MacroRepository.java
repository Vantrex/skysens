package de.vantrex.skysens.client.features.macro;

import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.features.macro.model.MacroJson;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Saved to config/skysens/repositories/MacroRepository.json. Don't rename the class or its fields, they are the file
 * name and the JSON keys. Workflows are kept as raw JSON objects and parsed by {@link MacroJson}, so nodes of an
 * unknown type survive a save untouched.
 */
@NoArgsConstructor
public class MacroRepository {

    @Expose
    public int version = MacroJson.CURRENT_VERSION;

    @Expose
    public List<JsonObject> workflows = new ArrayList<>();

}
