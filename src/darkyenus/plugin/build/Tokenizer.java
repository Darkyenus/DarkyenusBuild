package darkyenus.plugin.build;

/**
 *
 * @author Darkyen
 */
public class Tokenizer {
    private String[] arguments;
    private int position = -1;
    
    public Tokenizer(String[] arguments){
        this.arguments = arguments;
    }
    
    public boolean hasNext(){
        return position + 1 < arguments.length;
    }
    
    public String peek(){
        if(hasNext()){
            return arguments[position+1];
        }else{
            throw new IllegalStateException("Tokenizer does not have next!");
        }
    }
    
    public String next(){
        if(hasNext()){
            return arguments[++position];
        }else{
            throw new IllegalStateException("Tokenizer does not have next!");
        }
    }

    public int mark(){
        return position;
    }

    public void rollback(int mark){
        this.position = mark;
    }

    public String serialize() {
        if(position + 1 >= arguments.length) return "";
        if(position + 2 == arguments.length) return arguments[position+1];

        final StringBuilder sb = new StringBuilder();
        for (int i = position+1; i < arguments.length; i++) {
            sb.append(arguments[i]).append(' ');
        }
        sb.setLength(sb.length()-1);
        return sb.toString();
    }
}
