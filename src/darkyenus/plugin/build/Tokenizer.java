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
        return position + 1<arguments.length;
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
}
