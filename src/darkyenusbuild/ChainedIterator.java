package darkyenusbuild;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 *
 * @author Darkyen
 */
public class ChainedIterator <T> implements Iterator<T> {
    
    Iterator<T>[] iterators;
    int inIterator = 0;
    
    public ChainedIterator(Iterator<T>...iterators){
        this.iterators = iterators;
        doEmptyCheck();
    }
    
    public ChainedIterator(Iterator<Iterator<T>> iterators){
        ArrayList<Iterator<T>> iteratorsList = new ArrayList<Iterator<T>>();
        while(iterators.hasNext()){
            iteratorsList.add(iterators.next());
        }
        this.iterators = (Iterator<T>[])iteratorsList.toArray(new Iterator[iteratorsList.size()]);
        doEmptyCheck();
    }
    
    private void doEmptyCheck(){
        if(iterators.length == 0){
            iterators = new Iterator[1];
            iterators[0] = new Iterator<T>() {

                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public T next() {
                    throw new NoSuchElementException("Iteration has no more elements.");
                }

                @Override
                public void remove() {
                    throw new NoSuchElementException("Iteration has no more elements.");
                }
            };
        }
    }

    @Override
    public boolean hasNext() {
        if(iterators[inIterator].hasNext()){
            return true;
        }else{
            for(int i = inIterator + 1;i<iterators.length;i++){
                if(iterators[i].hasNext()){
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public T next() {
        if(iterators[inIterator].hasNext()){
            return iterators[inIterator].next();
        }else{
            for(int i = inIterator + 1;i<iterators.length;i++){
                if(iterators[i].hasNext()){
                    inIterator = i;
                    return iterators[i].next();
                }
            }
            throw new NoSuchElementException("Iteration has no more elements.");
        }
    }

    @Override
    public void remove() {
        iterators[inIterator].remove();
    }
    
}
