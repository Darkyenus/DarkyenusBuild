package darkyenusbuild;

import org.bukkit.Material;
import org.bukkit.block.Biome;

import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
public final class AttributeMatcher {

    public static Result matchAttribute(Tokenizer tokenizer) {
        if(!tokenizer.hasNext()) {
            return new Result("Worker specification is missing");
        }

        String parameter = tokenizer.next().toLowerCase();

        boolean blockTool = true;

        switch (parameter) {
            case "block":
                parameter = tokenizer.next().toLowerCase();
                blockTool = true;
                break;
            case "biome":
                parameter = tokenizer.next().toLowerCase();
                blockTool = false;
                break;
        }

        final List<Material> matchingMaterials;
        final String blockData;
        final List<Biome> matchingBiomes;

        {//Matching
            final int dataSplit = parameter.indexOf(':');
            if(dataSplit == -1){
                matchingMaterials = EnumMatcher.match(Material.class, parameter);
                blockData = "";
            } else {
                matchingMaterials = EnumMatcher.match(Material.class, parameter.substring(0, dataSplit));
                blockData = parameter.substring(dataSplit + 1);
            }
            matchingBiomes = EnumMatcher.match(Biome.class, parameter);
        }

        //Resolution
        if(!blockTool){
            if(matchingBiomes.size() == 1){
                return new Result(ResultType.BIOME, null, 0, matchingBiomes.get(0), null);
            } else if(matchingBiomes.size() == 0){
                if(matchingMaterials.size() != 0){
                    return new Result("Could not match biome named \""+parameter+"\", did you mean material "+String.join(" or ", matchingMaterials.stream().map(Object::toString).collect(Collectors.toList()))+"?");
                } else {
                    return new Result("Could not match any biome named \""+parameter+"\"");
                }
            } else {
                return new Result("Could not match single biome named \""+parameter+"\", did you mean "+String.join(" or ", matchingBiomes.stream().map(Object::toString).collect(Collectors.toList()))+"?");
            }
        } else {
            if(matchingMaterials.size() == 1){
                if(blockData.isEmpty()){
                    return new Result(ResultType.MATERIAL, matchingMaterials.get(0), 0, null, null);
                } else {
                    try {
                        final int dataInt = Integer.parseInt(blockData);
                        return new Result(ResultType.MATERIAL_WITH_DATA, matchingMaterials.get(0), dataInt, null, null);
                    } catch (NumberFormatException e) {
                        return new Result("Unrecognized data number \""+blockData+"\"");
                    }
                }
            } else if(matchingMaterials.size() == 0){
                if(matchingBiomes.size() != 0){
                    return new Result("Could not match material named \""+parameter+"\", did you mean biome "+String.join(" or ", matchingBiomes.stream().map(Object::toString).collect(Collectors.toList()))+"?");
                } else {
                    throw new ParsingUtils.SyntaxException("Could not match any material named \""+parameter+"\"");
                }
            } else {
                throw new ParsingUtils.SyntaxException("Could not match single material named \""+parameter+"\", did you mean "+String.join(" or ", matchingMaterials.stream().map(Object::toString).collect(Collectors.toList()))+"?");
            }
        }
    }

    public enum ResultType {
        MATERIAL,
        MATERIAL_WITH_DATA,
        BIOME,
        ERROR
    }

    public static final class Result {
        public final ResultType type;
        public final Material material;
        public final int data;
        public final Biome biome;
        public final String error;

        private Result(ResultType type, Material material, int data, Biome biome, String error) {
            this.type = type;
            this.material = material;
            this.data = data;
            this.biome = biome;
            this.error = error;
        }

        private Result(String error){
            this(ResultType.ERROR, null, 0, null, error);
        }

        @Override
        public String toString() {
            return "Result{" +
                    "type=" + type +
                    ", material=" + material +
                    ", data=" + data +
                    ", biome=" + biome +
                    ", error='" + error + '\'' +
                    '}';
        }
    }
}
