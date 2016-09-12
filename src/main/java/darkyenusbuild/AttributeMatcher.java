package darkyenusbuild;

import org.bukkit.Material;
import org.bukkit.block.Biome;

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

        final MatchUtils.MaterialSpec materialMatch;
        final MatchUtils.StringBuilderCommandSender materialMatchError = new MatchUtils.StringBuilderCommandSender();
        final MatchUtils.MatchResult<Biome> biomeMatch;
        {//Matching
            materialMatch = MatchUtils.matchMaterialData(parameter, materialMatchError);
            biomeMatch = MatchUtils.match(Biome.class, parameter);
        }

        //Resolution
        if (blockTool) {
            if (materialMatch != null) {
                if(materialMatch.hasData) {
                    return new Result(ResultType.MATERIAL_WITH_DATA, materialMatch.material, materialMatch.data, null, null);
                } else {
                    return new Result(ResultType.MATERIAL, materialMatch.material, materialMatch.data, null, null);
                }
            } else {
                return new Result(materialMatchError.sb.toString());
            }
        } else {
            if (biomeMatch.isDefinite) {
                return new Result(ResultType.BIOME, null, 0, biomeMatch.result(), null);
            } else {
                final MatchUtils.StringBuilderCommandSender cs = new MatchUtils.StringBuilderCommandSender();
                MatchUtils.matchFail("Biome", biomeMatch, cs);
                return new Result(cs.sb.toString());
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
