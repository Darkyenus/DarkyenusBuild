package darkyenus.plugin.build;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Darkyen
 */
public abstract class Filter {

    private LogicOperator operator = null;
    private Filter nextFilterPart;

    public static boolean passesFilter(Filter filter, Block block) {
        boolean accumulator = filter.passesFilter(block);
        while(filter.operator != null){
            accumulator = filter.operator.result(accumulator, filter.nextFilterPart.passesFilter(block));
            filter = filter.nextFilterPart;
        }
        return accumulator;
    }

    public String getInfo() {
        if(nextFilterPart == null){
            return getInfoRaw();
        } else {
            return getInfoRaw() + " " + operator + " " + nextFilterPart.getInfo();
        }
    }

    protected abstract String getInfoRaw();

    abstract boolean passesFilter(Block block);

    public static Filter createFilterExpression(Tokenizer tokenizer){
        final Filter root = createFilterUnary(tokenizer);
        Filter head = root;
        while(true){
            final LogicOperator operator = LogicOperator.matchLogicOperator(tokenizer);
            if(operator == null){
                return root;
            } else {
                head.operator = operator;
                final Filter nextFilter = createFilterUnary(tokenizer);
                head.nextFilterPart = nextFilter;
                head = nextFilter;
            }
        }
    }

    private static Filter createFilterUnary(Tokenizer tokenizer) {
        if(!tokenizer.hasNext()){
            throw new ParsingUtils.SyntaxException("Incomplete filter expression");
        }
        final String peek = tokenizer.peek();
        if(peek.equals("!") || peek.equalsIgnoreCase("NOT")){
            tokenizer.next();
            final Filter inverted = createFilterAtom(tokenizer);
            return new Filter() {

                @Override
                protected String getInfoRaw() {
                    return "not "+inverted.getInfo();
                }

                @Override
                protected boolean passesFilter(Block block) {
                    return !inverted.passesFilter(block);
                }
            };
        } else return createFilterAtom(tokenizer);
    }

    private static final Pattern COORDINATE_MATCHER = Pattern.compile("^([xyz])(<|>|<=|>=|==|!=)(-?\\d+|sea)$");

    private static Filter createFilterAtom(Tokenizer tokenizer) {
        if(tokenizer.hasNext()){
            final String coordinateFilter = tokenizer.peek();
            final Matcher matcher = COORDINATE_MATCHER.matcher(coordinateFilter);
            if(matcher.matches()){
                tokenizer.next();

                final String left = matcher.group(1);
                final String operator = matcher.group(2);
                final String right = matcher.group(3);

                return new Filter() {
                    @Override
                    protected String getInfoRaw() {
                        return coordinateFilter;
                    }

                    @Override
                    boolean passesFilter(Block block) {
                        final int leftOperand;
                        switch (left) {
                            case "x":
                                leftOperand = block.getX();
                                break;
                            case "y":
                                leftOperand = block.getY();
                                break;
                            case "z":
                                leftOperand = block.getZ();
                                break;
                            default:
                                throw new IllegalStateException("Regex failed. Left operand is "+left+". I'm heading to Tibet.");
                        }

                        final int rightOperand;
                        if(right.equalsIgnoreCase("sea")){
                            rightOperand = block.getWorld().getSeaLevel();
                        } else {
                            rightOperand = Integer.parseInt(right);
                        }

                        switch (operator) {
                            case "<":
                                return leftOperand < rightOperand;
                            case ">":
                                return leftOperand > rightOperand;
                            case "<=":
                                return leftOperand <= rightOperand;
                            case ">=":
                                return leftOperand >= rightOperand;
                            case "==":
                                return leftOperand == rightOperand;
                            case "!=":
                                return leftOperand != rightOperand;
                            default:
                                throw new IllegalStateException("Regex failed. Operator is "+operator+".");
                        }
                    }
                };
            }
        }

        final AttributeMatcher.Result result = AttributeMatcher.matchAttribute(tokenizer);
        switch (result.type) {
            case MATERIAL:
                return createMaterialFilter(result.material);
            case MATERIAL_WITH_DATA:
                return createMaterialAndDataFilter(result.material, result.data);
            case BIOME:
                return createBiomeFilter(result.biome);
            case ERROR:
                throw new ParsingUtils.SyntaxException(result.error);
            default:
                throw new ParsingUtils.SyntaxException("Something went wrong, matcher returned "+result);
        }
    }

    private static Filter createMaterialFilter(Material material) {
        return new Filter() {
            @Override
            protected boolean passesFilter(Block block) {
                return block.getType() == material;
            }

            @Override
            protected String getInfoRaw() {
                return "must be " + material;
            }
        };
    }

    private static Filter createMaterialAndDataFilter(Material material, int data) {
        return new Filter() {
            @Override
            protected boolean passesFilter(Block block) {
                //noinspection deprecation
                return block.getType() == material && block.getData() == data;
            }

            @Override
            protected String getInfoRaw() {
                return "must be " + material+" and data "+data;
            }
        };
    }

    private static Filter createBiomeFilter(Biome biome) {
        return new Filter() {
            @Override
            protected boolean passesFilter(Block block) {
                return block.getBiome() == biome;
            }

            @Override
            protected String getInfoRaw() {
                return "must be in " + biome;
            }
        };
    }

    private enum LogicOperator {

        AND {
            @Override
            public boolean result(boolean first, boolean second) {
                return first && second;
            }
        },
        OR {
            @Override
            public boolean result(boolean first, boolean second) {
                return first || second;
            }
        },
        NOR {
            @Override
            public boolean result(boolean first, boolean second) {
                return !first && !second;
            }
        },
        XOR {
            @Override
            public boolean result(boolean first, boolean second) {
                return (first || second) && first != second;
            }
        };

        public abstract boolean result(boolean first, boolean second);

        public static LogicOperator matchLogicOperator(Tokenizer tokenizer) {
            if(!tokenizer.hasNext()) return null;
            final String from = tokenizer.peek().toLowerCase();
            if (ParsingUtils.startsWith(from, "and", ",", "&&", "&")) {
                tokenizer.next();
                return AND;
            } else if (ParsingUtils.startsWith(from, "or", "/", "||", "|")) {
                tokenizer.next();
                return OR;
            } else if (ParsingUtils.startsWith(from, "nor")) {
                tokenizer.next();
                return NOR;
            } else if (ParsingUtils.startsWith(from, "xor")) {
                tokenizer.next();
                return XOR;
            } else {
                return null;
            }
        }
    }
}
